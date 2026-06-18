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
public class UserRoleSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        makeLegacyRoleColumnNullable();
    }

    private void makeLegacyRoleColumnNullable() {
        if (!columnExists("utilisateur", "role")) {
            return;
        }

        try {
            jdbcTemplate.execute("ALTER TABLE utilisateur MODIFY COLUMN role VARCHAR(255) NULL");
        } catch (DataAccessException ex) {
            log.warn("Could not make utilisateur.role nullable: {}", ex.getMessage());
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }
}
