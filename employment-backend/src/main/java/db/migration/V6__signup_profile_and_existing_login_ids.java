package db.migration;

import java.sql.*;
import java.util.*;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V6__signup_profile_and_existing_login_ids extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection c = context.getConnection();
        try (Statement statement = c.createStatement()) {
            statement.execute("ALTER TABLE users ADD COLUMN birth_date DATE");
            statement.execute(
                "ALTER TABLE users ADD COLUMN gender VARCHAR(10) CHECK (gender IN ('MALE','FEMALE'))"
            );
            statement.execute(
                "UPDATE users SET birth_date=(SELECT s.birth_date FROM job_seekers s WHERE s.user_id=users.user_id) WHERE EXISTS (SELECT 1 FROM job_seekers s WHERE s.user_id=users.user_id)"
            );
        }
        // Keep existing IDs. Accounts created before login IDs get a unique member<userId> ID.
        Set<String> taken = new HashSet<>();
        List<Long> missing = new ArrayList<>();
        try (
            Statement statement = c.createStatement();
            ResultSet rows = statement.executeQuery(
                "SELECT user_id,login_id FROM users ORDER BY user_id"
            )
        ) {
            while (rows.next()) {
                String id = rows.getString("login_id");
                if (id == null || id.isBlank()) missing.add(rows.getLong("user_id"));
                else taken.add(id.toLowerCase(Locale.ROOT));
            }
        }
        for (long userId : missing) {
            String base = "member" + userId;
            String id = base;
            int suffix = 1;
            while (!taken.add(id)) id = base + "x" + suffix++;
            try (
                PreparedStatement statement = c.prepareStatement(
                    "UPDATE users SET login_id=? WHERE user_id=?"
                )
            ) {
                statement.setString(1, id);
                statement.setLong(2, userId);
                statement.executeUpdate();
            }
        }
    }
}
