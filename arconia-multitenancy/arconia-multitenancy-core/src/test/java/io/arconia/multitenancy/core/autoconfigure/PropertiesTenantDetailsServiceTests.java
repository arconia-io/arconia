package io.arconia.multitenancy.core.autoconfigure;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link PropertiesTenantDetailsService}.
 */
class PropertiesTenantDetailsServiceTests {

    @Test
    void whenNullPropertiesThenThrow() {
        assertThatThrownBy(() -> new PropertiesTenantDetailsService(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantDetailsProperties cannot be null");
    }

    @Test
    void whenNullIdentifierThenThrow() {
        var tenantDetailsService = new PropertiesTenantDetailsService(new TenantDetailsProperties());

        assertThatThrownBy(() -> tenantDetailsService.loadTenantByIdentifier(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("identifier cannot be null or empty");
    }

    @Test
    void loadAllTenants() {
        var tenantDetailsProperties = new TenantDetailsProperties();
        tenantDetailsProperties.getTenants().addAll(List.of(buildTenantConfig("acme", true), buildTenantConfig("sam", false)));

        var tenantDetailsService = new PropertiesTenantDetailsService(tenantDetailsProperties);
        var tenants = tenantDetailsService.loadAllTenants();

        assertThat(tenants).isNotNull();
        assertThat(tenants).hasSize(2);
    }

    @Test
    void whenTenantEnabledThenReturn() {
        var tenantDetailsProperties = new TenantDetailsProperties();
        tenantDetailsProperties.getTenants().addAll(List.of(buildTenantConfig("acme", true)));

        var tenantDetailsService = new PropertiesTenantDetailsService(tenantDetailsProperties);
        var tenant = tenantDetailsService.loadTenantByIdentifier("acme");

        assertThat(tenant).isNotNull();
    }

    @Test
    void whenTenantDisabledThenReturn() {
        var tenantDetailsProperties = new TenantDetailsProperties();
        tenantDetailsProperties.getTenants().addAll(List.of(buildTenantConfig("acme", false)));

        var tenantDetailsService = new PropertiesTenantDetailsService(tenantDetailsProperties);
        var tenant = tenantDetailsService.loadTenantByIdentifier("acme");

        assertThat(tenant).isNotNull();
    }

    @Test
    void whenTenantUnknownThenNull() {
        var tenantDetailsProperties = new TenantDetailsProperties();
        tenantDetailsProperties.getTenants().addAll(List.of(buildTenantConfig("acme", true)));

        var tenantDetailsService = new PropertiesTenantDetailsService(tenantDetailsProperties);

        assertThat(tenantDetailsService.loadTenantByIdentifier("unknown")).isNull();
    }

    @Test
    void whenTenantLoadedThenCarriesConfiguredState() {
        var tenantDetailsProperties = new TenantDetailsProperties();
        tenantDetailsProperties.getTenants().addAll(List.of(buildTenantConfig("acme", true), buildTenantConfig("beans", false)));

        var tenantDetailsService = new PropertiesTenantDetailsService(tenantDetailsProperties);
        var tenant = tenantDetailsService.loadTenantByIdentifier("beans");

        assertThat(tenant).isNotNull();
        assertThat(tenant.identifier()).isEqualTo("beans");
        assertThat(tenant.enabled()).isFalse();
    }

    private TenantDetailsProperties.TenantConfig buildTenantConfig(String identifier, boolean enabled) {
        var tenantConfig = new TenantDetailsProperties.TenantConfig();
        tenantConfig.setIdentifier(identifier);
        tenantConfig.setEnabled(enabled);
        return tenantConfig;
    }

}
