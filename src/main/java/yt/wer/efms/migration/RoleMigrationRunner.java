package yt.wer.efms.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RoleMigrationRunner implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(RoleMigrationRunner.class);
    private final JdbcTemplate jdbcTemplate;

    public RoleMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("ALTER TABLE farm_users DROP CONSTRAINT IF EXISTS farm_users_role_check");
            jdbcTemplate.update("UPDATE farm_users SET role='editor' WHERE role IN ('share','SHARE')");
            jdbcTemplate.update("UPDATE farm_users SET role='viewer' WHERE role IN ('user','USER')");
            jdbcTemplate.update("UPDATE farm_users SET role=lower(role) WHERE role IN ('ADMIN','EDITOR','VIEWER')");
            jdbcTemplate.execute("ALTER TABLE farm_users ADD CONSTRAINT farm_users_role_check CHECK (role in ('admin','editor','viewer'))");

            jdbcTemplate.execute("ALTER TABLE parcel_shares DROP CONSTRAINT IF EXISTS parcel_shares_role_check");
            jdbcTemplate.update("UPDATE parcel_shares SET role='editor' WHERE role IN ('share','SHARE')");
            jdbcTemplate.update("UPDATE parcel_shares SET role='viewer' WHERE role IN ('user','USER')");
            jdbcTemplate.update("UPDATE parcel_shares SET role=lower(role) WHERE role IN ('EDITOR','VIEWER')");
            jdbcTemplate.execute("ALTER TABLE parcel_shares ADD CONSTRAINT parcel_shares_role_check CHECK (role in ('editor','viewer'))");
        } catch (Exception ex) {
            logger.warn("Role migration skipped: {}", ex.getMessage());
        }
    }
}