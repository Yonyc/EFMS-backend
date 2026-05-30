package yt.wer.efms.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class SchemaCleanupRunner implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(SchemaCleanupRunner.class);
    private final JdbcTemplate jdbcTemplate;

    public SchemaCleanupRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        backfillStatus("parcels");
        backfillStatus("tools");
        backfillStatus("products");
        backfillStatus("parcel_operations");

        drop("parcels", "period");
        drop("parcels", "active");
        drop("parcels", "start_validity");
        drop("parcels", "end_validity");

        drop("tools", "import_validated");
        drop("products", "import_validated");
        drop("parcel_operations", "import_validated");

        drop("parcels", "corresponding_pac");

        dropTable("imported_parcels");

        dropNotNull("parcel_periods", "period_id");

        createIndexes();
    }

    private void createIndexes() {
        createIndex("idx_parcels_geodata_gist",
                "CREATE INDEX IF NOT EXISTS idx_parcels_geodata_gist ON parcels USING GIST (ST_SetSRID(geodata, 4326))");
        createIndex("idx_parcels_farm_status",
                "CREATE INDEX IF NOT EXISTS idx_parcels_farm_status ON parcels (farm, status)");
        createIndex("idx_parcel_periods_parcel_id",
                "CREATE INDEX IF NOT EXISTS idx_parcel_periods_parcel_id ON parcel_periods (parcel_id)");
        createIndex("idx_parcel_periods_period_id",
                "CREATE INDEX IF NOT EXISTS idx_parcel_periods_period_id ON parcel_periods (period_id)");
    }

    private void createIndex(String name, String ddl) {
        try {
            jdbcTemplate.execute(ddl);
            logger.info("Schema cleanup: ensured index {}", name);
        } catch (Exception ex) {
            logger.debug("Schema cleanup: could not create index {}: {}", name, ex.getMessage());
        }
    }

    private void dropNotNull(String table, String column) {
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " ALTER COLUMN " + column + " DROP NOT NULL");
            logger.info("Schema cleanup: dropped NOT NULL on {}.{}", table, column);
        } catch (Exception ex) {
            logger.debug("Schema cleanup: could not drop NOT NULL on {}.{}: {}", table, column, ex.getMessage());
        }
    }

    private void dropTable(String table) {
        try {
            jdbcTemplate.execute("DROP TABLE IF EXISTS " + table + " CASCADE");
            logger.info("Schema cleanup: dropped table {} (if it existed)", table);
        } catch (Exception ex) {
            logger.warn("Schema cleanup: could not drop table {}: {}", table, ex.getMessage());
        }
    }

    private void drop(String table, String column) {
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " DROP COLUMN IF EXISTS " + column);
            logger.info("Schema cleanup: dropped {}.{} (if it existed)", table, column);
        } catch (Exception ex) {
            logger.warn("Schema cleanup: could not drop {}.{}: {}", table, column, ex.getMessage());
        }
    }

    private void backfillStatus(String table) {
        try {
            int updated = jdbcTemplate.update("UPDATE " + table + " SET status = 'LIVE' WHERE status IS NULL");
            if (updated > 0) {
                logger.info("Schema cleanup: backfilled status='LIVE' on {} rows in {}", updated, table);
            }
        } catch (Exception ex) {
            logger.debug("Schema cleanup: status backfill skipped for {}: {}", table, ex.getMessage());
        }
    }
}
