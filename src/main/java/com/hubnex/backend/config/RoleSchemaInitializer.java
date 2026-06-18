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
public class RoleSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        addColumnIfMissing("system_role", "ALTER TABLE roles ADD COLUMN system_role BIT(1) NOT NULL DEFAULT b'0'");
        executeSafely("""
                UPDATE roles
                SET system_role = b'1', active = 1
                WHERE name = 'ADMIN'
                """);
        executeSafely("""
                UPDATE roles
                SET system_role = b'0', active = 0
                WHERE name <> 'ADMIN'
                  AND (name LIKE 'AGENT\\_%' OR name = CONCAT('CON', 'VOYEUR'))
                """);
        executeSafely("""
                UPDATE utilisateur
                SET role = NULL
                WHERE role IS NOT NULL AND role <> 'ADMIN'
                """);
        updateRoleTypeColumnIfPresent("role_type");
        updateRoleTypeColumnIfPresent("type");
    }

    private void updateRoleTypeColumnIfPresent(String columnName) {
        if (!columnExists(columnName)) {
            return;
        }

        String escapedColumn = "`" + columnName + "`";
        executeSafely("""
                UPDATE roles
                SET %s = 'SYSTEM'
                WHERE UPPER(REPLACE(name, '-', '_')) IN ('ADMIN', 'ROLE_ADMIN', 'SUP_ADMIN')
                """.formatted(escapedColumn));
        executeSafely("""
                UPDATE roles
                SET %s = 'DYNAMIC'
                WHERE UPPER(REPLACE(name, '-', '_')) NOT IN ('ADMIN', 'ROLE_ADMIN', 'SUP_ADMIN')
                """.formatted(escapedColumn));
    }

    private void addColumnIfMissing(String columnName, String alterSql) {
        if (columnExists(columnName)) {
            return;
        }
        executeSafely(alterSql);
    }

    private boolean columnExists(String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'roles'
                  AND column_name = ?
                """, Integer.class, columnName);
        return count != null && count > 0;
    }

    private void executeSafely(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (DataAccessException ex) {
            log.warn("Role schema initialization skipped SQL [{}]: {}", sql, ex.getMessage());
        }
    }
}
