package io.arconia.multitenancy.tenantdetails.jdbc;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Integration tests for {@link JdbcTenantDetailsService}.
 */
@EnabledIfDockerAvailable
@Testcontainers
class JdbcTenantDetailsServiceIT {

    @Container
    static final PostgreSQLContainer postgresql = new PostgreSQLContainer("postgres:18.4-alpine");

    private static JdbcTenantDetailsService tenantDetailsService;

    @BeforeAll
    static void setUp() {
        var dataSource = new DriverManagerDataSource(postgresql.getJdbcUrl(), postgresql.getUsername(),
                postgresql.getPassword());
        var populator = new ResourceDatabasePopulator(
                new ClassPathResource("io/arconia/multitenancy/tenantdetails/jdbc/autoconfigure/schema-postgresql.sql"),
                new ClassPathResource("tenant-details.sql"));
        populator.execute(dataSource);
        tenantDetailsService = JdbcTenantDetailsService.builder().jdbcClient(JdbcClient.create(dataSource)).build();
    }

    @Test
    void loadAllTenants() {
        var tenants = tenantDetailsService.loadAllTenants();

        assertThat(tenants).hasSize(3);
        assertThat(tenants).extracting("identifier").containsExactlyInAnyOrder("acme", "beans", "pixie");
    }

    @Test
    void whenTenantEnabledThenReturn() {
        var tenant = tenantDetailsService.loadTenantByIdentifier("acme");

        assertThat(tenant).isNotNull();
        assertThat(tenant.enabled()).isTrue();
        assertThat(tenant.attributes()).containsOnly(entry("plan", "premium"), entry("region", "eu-north-1"));
    }

    @Test
    void whenTenantDisabledThenReturn() {
        var tenant = tenantDetailsService.loadTenantByIdentifier("pixie");

        assertThat(tenant).isNotNull();
        assertThat(tenant.enabled()).isFalse();
    }

    @Test
    void whenTenantNotFoundThenNull() {
        assertThat(tenantDetailsService.loadTenantByIdentifier("unknown")).isNull();
    }

}
