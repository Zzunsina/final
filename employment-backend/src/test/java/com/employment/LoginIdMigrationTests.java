package com.employment;

import static org.assertj.core.api.Assertions.*;

import java.sql.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class LoginIdMigrationTests {

    @Test
    void migrationPreservesExistingIdsAndAssignsCollisionFreeIdsToOlderAccounts() throws Exception {
        String url =
            "jdbc:h2:mem:migration-login-ids;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url, "sa", "").target("5").load().migrate();
        try (
            Connection c = DriverManager.getConnection(url, "sa", "");
            Statement s = c.createStatement()
        ) {
            s.execute(
                "INSERT INTO users(user_id,email,password,name,phone,role) VALUES(1,'old@test.local','hash','기존회원','01012345678','JOB_SEEKER')"
            );
            s.execute(
                "INSERT INTO users(user_id,login_id,email,password,name,phone,role) VALUES(2,'member1','taken@test.local','hash','아이디회원','01012345678','COMPANY')"
            );
            s.execute("INSERT INTO job_seekers(user_id,birth_date) VALUES(1,'2000-01-23')");
            Flyway.configure().dataSource(url, "sa", "").load().migrate();
            try (
                ResultSet rows = s.executeQuery(
                    "SELECT login_id,birth_date,gender FROM users ORDER BY user_id"
                )
            ) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("member1x1");
                assertThat(rows.getDate(2).toString()).isEqualTo("2000-01-23");
                assertThat(rows.getString(3)).isNull();
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("member1");
                assertThat(rows.next()).isFalse();
            }
        }
    }
}
