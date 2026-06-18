package com.hubnex.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AgencySchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        dropColumnIfExists("ville");
    }

    private void dropColumnIfExists(String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'agence'
                  AND column_name = ?
                """, Integer.class, columnName);

        if (count != null && count > 0) {
            jdbcTemplate.execute("ALTER TABLE agence DROP COLUMN " + columnName);
        }
    }
}
