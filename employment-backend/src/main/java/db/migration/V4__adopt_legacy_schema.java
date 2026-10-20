package db.migration;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.core.io.ClassPathResource;

/** Adds missing features to the original twelve-table schema without replacing its rows. */
public class V4__adopt_legacy_schema extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        add(connection, "companies", "review_reason", "TEXT");
        add(connection, "companies", "reviewed_by", "BIGINT");
        add(connection, "companies", "reviewed_at", "TIMESTAMP NULL");
        add(connection, "job_postings", "review_reason", "TEXT");
        add(connection, "job_postings", "reviewed_by", "BIGINT");
        add(connection, "job_postings", "reviewed_at", "TIMESTAMP NULL");
        add(connection, "employment_links", "public_message", "TEXT");
        add(connection, "interviews", "request_key", "VARCHAR(100)");
        add(connection, "interviews", "request_hash", "VARCHAR(64)");
        add(connection, "resumes", "updated_at", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");

        boolean maria =
            connection
                .getMetaData()
                .getDatabaseProductName()
                .toLowerCase(Locale.ROOT)
                .contains("maria") ||
            connection
                .getMetaData()
                .getDatabaseProductName()
                .toLowerCase(Locale.ROOT)
                .contains("mysql");
        if (maria) {
            execute(
                connection,
                "ALTER TABLE companies MODIFY company_size VARCHAR(100), MODIFY website VARCHAR(500)"
            );
            execute(connection, "ALTER TABLE interviews MODIFY result TEXT, MODIFY memo TEXT");
            execute(
                connection,
                "ALTER TABLE inquiries MODIFY status VARCHAR(20) NOT NULL DEFAULT 'OPEN'"
            );
        }
        execute(connection, "UPDATE inquiries SET status='OPEN' WHERE status='WAITING'");
        List<Long> legacyInterviews = new ArrayList<>();
        try (
            Statement statement = connection.createStatement();
            ResultSet rows = statement.executeQuery(
                "SELECT interview_id FROM interviews WHERE request_key IS NULL OR request_hash IS NULL"
            )
        ) {
            while (rows.next()) legacyInterviews.add(rows.getLong(1));
        }
        for (long id : legacyInterviews) {
            try (
                PreparedStatement statement = connection.prepareStatement(
                    "UPDATE interviews SET request_key=?,request_hash=? WHERE interview_id=?"
                )
            ) {
                statement.setString(1, "legacy-" + id);
                statement.setString(
                    2,
                    com.employment.auth.AuthService.hash("legacy-interview-" + id)
                );
                statement.setLong(3, id);
                statement.executeUpdate();
            }
        }
        if (maria) execute(
            connection,
            "ALTER TABLE interviews MODIFY request_key VARCHAR(100) NOT NULL, MODIFY request_hash VARCHAR(64) NOT NULL"
        );
        createIndex(
            connection,
            "interviews",
            "uq_interview_request",
            "application_id,request_key",
            true
        );

        String security = new ClassPathResource(
            "db/migration/V2__security_and_snapshots.sql"
        ).getContentAsString(StandardCharsets.UTF_8);
        for (String sql : security.split(";")) {
            sql = sql.trim();
            if (sql.startsWith("CREATE TABLE")) {
                String table = sql.split("\\s+")[2];
                if (!hasTable(connection, table)) execute(connection, sql);
            } else if (sql.startsWith("CREATE INDEX")) {
                String name = sql.split("\\s+")[2];
                String tail = sql.substring(sql.indexOf(" ON ") + 4);
                String table = tail.substring(0, tail.indexOf('('));
                if (!hasIndex(connection, table, name)) execute(connection, sql);
            }
        }
        String checks = new ClassPathResource(
            "db/migration/V3__state_constraints.sql"
        ).getContentAsString(StandardCharsets.UTF_8);
        for (String sql : checks.split(";")) {
            sql = sql.trim();
            if (sql.isEmpty()) continue;
            String[] words = sql.split("\\s+");
            if (!hasConstraint(connection, words[2], words[5])) execute(connection, sql);
        }
        for (String table : List.of("companies", "job_postings")) {
            boolean found = false;
            try (
                ResultSet keys = connection
                    .getMetaData()
                    .getImportedKeys(connection.getCatalog(), connection.getSchema(), table)
            ) {
                while (keys.next()) if (
                    "reviewed_by".equalsIgnoreCase(keys.getString("FKCOLUMN_NAME"))
                ) found = true;
            }
            if (!found) execute(
                connection,
                "ALTER TABLE " +
                    table +
                    " ADD CONSTRAINT fk_" +
                    table +
                    "_reviewer FOREIGN KEY(reviewed_by) REFERENCES users(user_id)"
            );
        }
    }

    private void add(Connection c, String table, String column, String type) throws SQLException {
        try (
            ResultSet columns = c
                .getMetaData()
                .getColumns(c.getCatalog(), c.getSchema(), table, column)
        ) {
            if (columns.next()) return;
        }
        execute(c, "ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
    }

    private boolean hasTable(Connection c, String table) throws SQLException {
        try (
            ResultSet tables = c
                .getMetaData()
                .getTables(c.getCatalog(), c.getSchema(), table, new String[] { "TABLE" })
        ) {
            return tables.next();
        }
    }

    private boolean hasIndex(Connection c, String table, String name) throws SQLException {
        try (
            ResultSet indexes = c
                .getMetaData()
                .getIndexInfo(c.getCatalog(), c.getSchema(), table, false, false)
        ) {
            while (indexes.next()) if (
                name.equalsIgnoreCase(indexes.getString("INDEX_NAME"))
            ) return true;
        }
        return false;
    }

    private void createIndex(
        Connection c,
        String table,
        String name,
        String columns,
        boolean unique
    ) throws SQLException {
        if (!hasIndex(c, table, name)) execute(
            c,
            "CREATE " +
                (unique ? "UNIQUE " : "") +
                "INDEX " +
                name +
                " ON " +
                table +
                "(" +
                columns +
                ")"
        );
    }

    private boolean hasConstraint(Connection c, String table, String name) throws SQLException {
        try (
            PreparedStatement statement = c.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema=? AND table_name=? AND constraint_name=?"
            )
        ) {
            statement.setString(
                1,
                c.getMetaData().getDatabaseProductName().equalsIgnoreCase("H2")
                    ? c.getSchema()
                    : c.getCatalog()
            );
            statement.setString(2, table);
            statement.setString(3, name);
            try (ResultSet rows = statement.executeQuery()) {
                rows.next();
                return rows.getInt(1) > 0;
            }
        }
    }

    private void execute(Connection c, String sql) throws SQLException {
        try (Statement statement = c.createStatement()) {
            statement.execute(sql);
        }
    }
}
