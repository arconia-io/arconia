package io.arconia.multitenancy.tenantdetails.jdbc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

/**
 * Unit tests for {@link JdbcTenantDetailsService}.
 */
class JdbcTenantDetailsServiceTests {

    private EmbeddedDatabase dataSource;

    private JdbcTenantDetailsService tenantDetailsService;

    @BeforeEach
    void setUp() {
        dataSource = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
            .generateUniqueName(true)
            .addScript("io/arconia/multitenancy/tenantdetails/jdbc/autoconfigure/schema-h2.sql")
            .addScript("tenant-details.sql")
            .build();
        tenantDetailsService = JdbcTenantDetailsService.builder().dataSource(dataSource).build();
    }

    @AfterEach
    void tearDown() {
        dataSource.shutdown();
    }

    @Test
    void whenNullDataSourceThenThrow() {
        assertThatThrownBy(() -> JdbcTenantDetailsService.builder().dataSource(null).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("dataSource cannot be null");
    }

    @Test
    void whenNullJdbcClientThenThrow() {
        assertThatThrownBy(() -> JdbcTenantDetailsService.builder().build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("jdbcClient cannot be null");
    }

    @Test
    void loadAllTenants() {
        var tenants = tenantDetailsService.loadAllTenants();

        assertThat(tenants).isNotNull();
        assertThat(tenants).hasSize(3);
        assertThat(tenants).extracting("identifier").containsExactlyInAnyOrder("acme", "beans", "pixie");
    }

    @Test
    void whenTenantEnabledThenReturn() {
        var tenant = tenantDetailsService.loadTenantByIdentifier("acme");

        assertThat(tenant).isNotNull();
        assertThat(tenant.identifier()).isEqualTo("acme");
        assertThat(tenant.enabled()).isTrue();
    }

    @Test
    void whenTenantDisabledThenReturn() {
        var tenant = tenantDetailsService.loadTenantByIdentifier("pixie");

        assertThat(tenant).isNotNull();
        assertThat(tenant.identifier()).isEqualTo("pixie");
        assertThat(tenant.enabled()).isFalse();
    }

    @Test
    void whenTenantHasAttributesThenReturnAll() {
        var tenant = tenantDetailsService.loadTenantByIdentifier("acme");

        assertThat(tenant).isNotNull();
        assertThat(tenant.attributes()).containsOnly(entry("plan", "premium"), entry("region", "eu-north-1"));
    }

    @Test
    void whenTenantHasNoAttributesThenReturnEmpty() {
        var tenant = tenantDetailsService.loadTenantByIdentifier("beans");

        assertThat(tenant).isNotNull();
        assertThat(tenant.attributes()).isEmpty();
    }

    @Test
    void whenTenantNotFoundThenNull() {
        assertThat(tenantDetailsService.loadTenantByIdentifier("unknown")).isNull();
    }

    @Test
    void whenNullIdentifierThenThrow() {
        assertThatThrownBy(() -> tenantDetailsService.loadTenantByIdentifier(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("identifier cannot be null or empty");
    }

    @Test
    void whenEmptyIdentifierThenThrow() {
        assertThatThrownBy(() -> tenantDetailsService.loadTenantByIdentifier(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("identifier cannot be null or empty");
    }

}
