package com.hubnex.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BonReceptionSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        dropColumnIfExists("tracking", "bon_reception_id");
        dropTableIfExists("ligne_reception");
        dropTableIfExists("bon_reception");
    }

    private void dropTableIfExists(String tableName) {
        try {
            jdbcTemplate.execute("DROP TABLE IF EXISTS `" + tableName + "`");
        } catch (DataAccessException ex) {
            log.warn("Could not drop old reception table {}: {}", tableName, ex.getMessage());
        }
    }

    private void dropColumnIfExists(String tableName, String columnName) {
        if (!columnExists(tableName, columnName)) {
            return;
        }

        try {
            dropForeignKeysForColumn(tableName, columnName);
            jdbcTemplate.execute("ALTER TABLE `" + tableName + "` DROP COLUMN `" + columnName + "`");
        } catch (DataAccessException ex) {
            log.warn("Could not drop old reception column {}.{}: {}", tableName, columnName, ex.getMessage());
        }
    }

    private void dropForeignKeysForColumn(String tableName, String columnName) {
        List<String> foreignKeys = jdbcTemplate.queryForList("""
                SELECT constraint_name
                FROM information_schema.key_column_usage
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                  AND referenced_table_name IS NOT NULL
                """, String.class, tableName, columnName);

        for (String foreignKey : foreignKeys) {
            jdbcTemplate.execute("ALTER TABLE `" + tableName + "` DROP FOREIGN KEY `" + foreignKey + "`");
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
