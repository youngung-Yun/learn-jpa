package com.example.learnjpa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.sql.init.mode=always",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class DataInitializationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("schema.sql과 data.sql로 N+1 학습 데이터를 초기화한다")
    void initializeLearningData() {
        assertThat(count("members")).isEqualTo(4);
        assertThat(count("products")).isEqualTo(8);
        assertThat(count("orders")).isEqualTo(6);
        assertThat(count("order_products")).isEqualTo(14);
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table,
                Integer.class
        );
    }
}
