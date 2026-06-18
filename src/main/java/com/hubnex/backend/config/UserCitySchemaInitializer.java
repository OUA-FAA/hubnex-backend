package com.hubnex.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserCitySchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        executeSafely("""
                CREATE TABLE IF NOT EXISTS user_cities (
                    user_id BIGINT NOT NULL,
                    city_id BIGINT NOT NULL,
                    PRIMARY KEY (user_id, city_id)
                )
                """);
    }

    private void executeSafely(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (DataAccessException ex) {
            log.warn("User city schema initialization skipped SQL [{}]: {}", sql, ex.getMessage());
        }
    }
}
