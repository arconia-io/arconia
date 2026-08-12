package io.arconia.multitenancy.tenantdetails.jdbc;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

/**
 * Abstract base class for tests of {@link JdbcTenantDetailsService}, running the same
 * assertions against each database for which schema scripts are bundled with this module.
 * <p>
 * To cover an additional database, add {@code schema-<platform>.sql} and
 * {@code schema-drop-<platform>.sql} to the module and extend this class.
 */
abstract class BaseJdbcTenantDetailsServiceTests {

    private static final String SCRIPTS_LOCATION = "io/arconia/multitenancy/tenantdetails/jdbc/autoconfigure/";

    private JdbcTenantDetailsService tenantDetailsService;

    /**
     * The platform whose bundled schema scripts are under test.
     */
    protected abstract String getPlatform();

    /**
     * The data source for the database under test.
     */
    protected abstract DataSource getDataSource();

    @BeforeEach
    void initializeDatabase() {
        var populator = new ResourceDatabasePopulator(
                new ClassPathResource(SCRIPTS_LOCATION + "schema-drop-" + getPlatform() + ".sql"),
                new ClassPathResource(SCRIPTS_LOCATION + "schema-" + getPlatform() + ".sql"),
                new ClassPathResource("tenant-details.sql"));
        populator.setIgnoreFailedDrops(true);
        populator.execute(getDataSource());
        tenantDetailsService = JdbcTenantDetailsService.builder().dataSource(getDataSource()).build();
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
