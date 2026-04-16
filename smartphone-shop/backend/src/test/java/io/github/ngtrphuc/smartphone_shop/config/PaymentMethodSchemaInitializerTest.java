package io.github.ngtrphuc.smartphone_shop.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class PaymentMethodSchemaInitializerTest {

    @Test
    void migrate_shouldWidenLegacyH2EnumColumnToVarchar() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:payment_schema_initializer;MODE=MySQL;DB_CLOSE_DELAY=-1");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("""
                create table payment_methods (
                    id bigint auto_increment primary key,
                    active boolean not null,
                    created_at timestamp not null,
                    detail varchar(200),
                    is_default boolean not null,
                    type enum('BANK_TRANSFER', 'CASH_ON_DELIVERY', 'PAYPAY') not null,
                    user_email varchar(100) not null
                )
                """);

        PaymentMethodSchemaInitializer initializer = new PaymentMethodSchemaInitializer(jdbcTemplate, dataSource);
        initializer.migrate();

        jdbcTemplate.update("""
                insert into payment_methods (active, created_at, detail, is_default, type, user_email)
                values (true, CURRENT_TIMESTAMP, null, true, ?, ?)
                """, "VISA", "user@example.com");

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from payment_methods where type = ?",
                Integer.class,
                "VISA");

        assertEquals(1, count);
    }
}
