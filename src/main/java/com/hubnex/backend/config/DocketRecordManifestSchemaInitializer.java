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
public class DocketRecordManifestSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        addColumnIfMissing("manifest_name", "ALTER TABLE docket_record ADD COLUMN manifest_name VARCHAR(255) NULL");
        addColumnIfMissing("import_batch_id", "ALTER TABLE docket_record ADD COLUMN import_batch_id VARCHAR(255) NULL");
        addColumnIfMissing("imported_at", "ALTER TABLE docket_record ADD COLUMN imported_at DATETIME NULL");
    }

    private void addColumnIfMissing(String columnName, String alterSql) {
        if (columnExists(columnName)) {
            return;
        }

        try {
            jdbcTemplate.execute(alterSql);
        } catch (DataAccessException ex) {
            log.warn("Could not add docket_record column {}: {}", columnName, ex.getMessage());
        }
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
}
