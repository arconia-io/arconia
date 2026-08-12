package io.arconia.multitenancy.data.jdbc;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import io.arconia.multitenancy.core.context.TenantContext;
import io.arconia.multitenancy.core.exceptions.TenantNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for {@link TenantDataSource}, verifying that the same query is routed
 * to a different database depending on the tenant bound to the current context.
 */
class TenantDataSourceIT {

    private final EmbeddedDatabase acme = embeddedDatabase();

    private final EmbeddedDatabase beans = embeddedDatabase();

    private final EmbeddedDatabase admin = embeddedDatabase();

    @BeforeEach
    void initializeDatabases() {
        insertProduct(acme, "Anvil");
        insertProduct(beans, "Espresso");
        insertProduct(admin, "Console");
    }

    @AfterEach
    void shutdownDatabases() {
        acme.shutdown();
        beans.shutdown();
        admin.shutdown();
    }

    @Test
    void whenTenantBoundThenQueryRoutedToTenantDatabase() throws Exception {
        var tenantDataSource = TenantDataSource.builder().dataSource("acme", acme).dataSource("beans", beans).build();

        assertThat(productName(tenantDataSource, "acme")).isEqualTo("Anvil");
        assertThat(productName(tenantDataSource, "beans")).isEqualTo("Espresso");
    }

    @Test
    void whenTenantCreatedByFactoryThenQueryRoutedToTenantDatabase() throws Exception {
        var tenantDataSource = TenantDataSource.builder()
            .dataSourceFactory(tenantIdentifier -> switch (tenantIdentifier) {
                case "acme" -> acme;
                case "beans" -> beans;
                default -> throw new IllegalArgumentException("Unknown tenant: " + tenantIdentifier);
            })
            .build();

        assertThat(productName(tenantDataSource, "acme")).isEqualTo("Anvil");
        assertThat(productName(tenantDataSource, "beans")).isEqualTo("Espresso");
    }

    @Test
    void whenNoTenantBoundThenQueryRoutedToDefaultDatabase() {
        var tenantDataSource = TenantDataSource.builder()
            .dataSource("acme", acme)
            .defaultDataSource(admin)
            .build();

        assertThat(productName(tenantDataSource)).isEqualTo("Console");
    }

    @Test
    void whenNoTenantBoundAndNoDefaultThenThrow() {
        var tenantDataSource = TenantDataSource.builder().dataSource("acme", acme).build();

        assertThatThrownBy(tenantDataSource::getConnection).isInstanceOf(TenantNotFoundException.class)
            .hasMessageContaining("No tenant found in the current context");
    }

    @Test
    void whenNoTenantBoundAndNoDefaultThenJdbcClientReportsTheReason() {
        var tenantDataSource = TenantDataSource.builder().dataSource("acme", acme).build();

        // Callers going through JdbcClient or JdbcTemplate see the exception wrapped by
        // DataSourceUtils#getConnection, with the original preserved as the cause.
        assertThatThrownBy(() -> productName(tenantDataSource)).isInstanceOf(CannotGetJdbcConnectionException.class)
            .cause()
            .isInstanceOf(TenantNotFoundException.class)
            .hasMessageContaining("No tenant found in the current context");
    }

    private static String productName(TenantDataSource tenantDataSource, String tenantIdentifier) throws Exception {
        return TenantContext.where(tenantIdentifier).call(() -> productName(tenantDataSource));
    }

    private static String productName(DataSource dataSource) {
        return JdbcClient.create(dataSource).sql("select name from product").query(String.class).single();
    }

    private static void insertProduct(DataSource dataSource, String name) {
        JdbcClient.create(dataSource).sql("insert into product (name) values (?)").param(name).update();
    }

    private static EmbeddedDatabase embeddedDatabase() {
        return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
            .generateUniqueName(true)
            .addScript("product.sql")
            .build();
    }

}
