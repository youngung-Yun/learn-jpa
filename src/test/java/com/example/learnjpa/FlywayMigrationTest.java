package com.example.learnjpa;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:flyway-test",
        "spring.sql.init.mode=never",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class FlywayMigrationTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void applyV1AndV2() {
        var versions = jdbcTemplate.queryForList(
                """
                SELECT "version"
                FROM "flyway_schema_history"
                WHERE "success" = true
                ORDER BY "installed_rank"
                """,
                String.class
        );

        assertThat(versions).contains("1", "2");
    }
}
