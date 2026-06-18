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
public class DispatchSchemaCleanupInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        addColumnIfMissing("type_flux", "ALTER TABLE docket_record ADD COLUMN type_flux VARCHAR(50) NULL");
        executeSafely("ALTER TABLE docket_record MODIFY type_flux VARCHAR(50) NULL");
        dropTableIfExists("ligne_dispatch");
        dropTableIfExists("bon_dispatch");
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
                  AND table_name = 'docket_record'
                  AND column_name = ?
                """, Integer.class, columnName);
        return count != null && count > 0;
    }

    private void dropTableIfExists(String tableName) {
        if (!tableExists(tableName)) {
            return;
        }
        executeSafely("DROP TABLE " + tableName);
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                """, Integer.class, tableName);
        return count != null && count > 0;
    }

    private void executeSafely(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (DataAccessException ex) {
            log.warn("Dispatch schema cleanup skipped SQL [{}]: {}", sql, ex.getMessage());
        }
    }
}
