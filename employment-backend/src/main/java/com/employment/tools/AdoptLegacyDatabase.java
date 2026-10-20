package com.employment.tools;

import java.sql.*;
import java.util.*;
import org.flywaydb.core.Flyway;

/** Explicit one-time adoption; ordinary application startup never baselines automatically. */
public class AdoptLegacyDatabase {

    public static void main(String[] args) throws Exception {
        String url = System.getenv().getOrDefault(
            "DB_URL",
            "jdbc:mariadb://localhost:3306/employment_db"
        );
        String username = System.getenv().getOrDefault("DB_USERNAME", "employment_app");
        String password = Objects.requireNonNull(
            System.getenv("DB_PASSWORD"),
            "DB_PASSWORD is required"
        );
        boolean hasHistory = false;
        Set<String> actual = new HashSet<>();
        try (
            Connection connection = DriverManager.getConnection(url, username, password);
            ResultSet tables = connection
                .getMetaData()
                .getTables(connection.getCatalog(), null, "%", new String[] { "TABLE" })
        ) {
            while (tables.next()) actual.add(
                tables.getString("TABLE_NAME").toLowerCase(Locale.ROOT)
            );
            hasHistory = actual.contains("flyway_schema_history");
            Set<String> expected = Set.of(
                "users",
                "job_seekers",
                "companies",
                "job_postings",
                "resumes",
                "job_bookmarks",
                "applications",
                "interviews",
                "employment_links",
                "notices",
                "inquiries",
                "notifications"
            );
            if (!hasHistory && !actual.equals(expected)) {
                throw new IllegalStateException(
                    "Expected the reviewed original twelve-table schema; refusing automatic adoption."
                );
            }
        }
        Flyway flyway = Flyway.configure()
            .dataSource(url, username, password)
            .baselineVersion("3")
            .baselineDescription("Reviewed original twelve-table schema; upgraded by V4")
            .load();
        if (!hasHistory) flyway.baseline();
        flyway.migrate();
        flyway.validate();
        System.out.println("Legacy schema adoption completed; existing rows preserved.");
    }
}
