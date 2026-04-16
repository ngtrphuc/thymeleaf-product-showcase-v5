package io.github.ngtrphuc.smartphone_shop.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentMethodSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PaymentMethodSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public PaymentMethodSchemaInitializer(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        migrate();
    }

    void migrate() {
        String databaseProductName = detectDatabaseProductName();
        if (databaseProductName == null || databaseProductName.isBlank()) {
            return;
        }
        if (!hasTypeColumn()) {
            return;
        }

        String alterSql = alterSqlFor(databaseProductName);
        if (alterSql == null) {
            return;
        }

        try {
            jdbcTemplate.execute(alterSql);
            log.info("Ensured payment_methods.type is stored as VARCHAR for {}.", databaseProductName);
        } catch (DataAccessException ex) {
            log.warn("Skipping payment_methods.type schema compatibility update: {}", ex.getMostSpecificCause().getMessage());
        }
    }

    static String alterSqlFor(String databaseProductName) {
        String normalized = databaseProductName == null
                ? ""
                : databaseProductName.trim().toLowerCase(Locale.ROOT);

        if (normalized.contains("h2")) {
            return "ALTER TABLE payment_methods ALTER COLUMN type VARCHAR(40)";
        }
        if (normalized.contains("mysql") || normalized.contains("mariadb")) {
            return "ALTER TABLE payment_methods MODIFY COLUMN type VARCHAR(40) NOT NULL";
        }
        return null;
    }

    private boolean hasTypeColumn() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            String catalog = connection.getCatalog();
            if (columnExists(metadata, catalog, "PAYMENT_METHODS", "TYPE")) {
                return true;
            }
            return columnExists(metadata, catalog, "payment_methods", "type");
        } catch (SQLException ex) {
            log.debug("Unable to inspect payment_methods.type column metadata.", ex);
            return false;
        }
    }

    private boolean columnExists(DatabaseMetaData metadata, String catalog, String tableName, String columnName)
            throws SQLException {
        try (ResultSet resultSet = metadata.getColumns(catalog, null, tableName, columnName)) {
            return resultSet.next();
        }
    }

    private String detectDatabaseProductName() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName();
        } catch (SQLException ex) {
            log.debug("Unable to detect database product name for payment method migration.", ex);
            return null;
        }
    }
}
