package com.transit.reliability.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String dbProduct = connection.getMetaData().getDatabaseProductName();
            log.info("Detected database product: {}", dbProduct);

            if ("PostgreSQL".equalsIgnoreCase(dbProduct)) {
                log.info("Configuring TimescaleDB on PostgreSQL database...");
                
                // Enable TimescaleDB extension if not exists
                jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;");
                log.info("TimescaleDB extension enabled successfully.");

                // Convert trip_updates table to a hypertable partition on event_timestamp
                try {
                    jdbcTemplate.execute("SELECT create_hypertable('trip_updates', 'event_timestamp', if_not_exists => TRUE);");
                    log.info("Converted trip_updates table to TimescaleDB hypertable successfully.");
                } catch (Exception e) {
                    log.warn("Could not create hypertable (it might be already configured or table does not exist yet): {}", e.getMessage());
                }
            } else {
                log.info("Skipping TimescaleDB setup since the database is {}", dbProduct);
            }
        } catch (Exception e) {
            log.error("Error during database initialization: ", e);
        }
    }
}
