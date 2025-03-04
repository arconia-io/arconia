package io.arconia.multitenancy.autoconfigure.core.tenantdetails;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PropertiesTenantDetailsService}.
 */
class PropertiesTenantDetailsServiceTests {

    @Test
    void loadAllTenants() {
        var tenantDetailsProperties = new TenantDetailsProperties();
        tenantDetailsProperties.setTenants(List.of(buildTenantConfig("acme", true), buildTenantConfig("sam", false)));

        var tenantDetailsService = new PropertiesTenantDetailsService(tenantDetailsProperties);
        var tenants = tenantDetailsService.loadAllTenants();

        assertThat(tenants).isNotNull();
        assertThat(tenants).hasSize(2);
    }

    @Test
    void whenTenantEnabledThenReturn() {
        var tenantDetailsProperties = new TenantDetailsProperties();
        tenantDetailsProperties.setTenants(List.of(buildTenantConfig("acme", true)));

        var tenantDetailsService = new PropertiesTenantDetailsService(tenantDetailsProperties);
        var tenant = tenantDetailsService.loadTenantByIdentifier("acme");

        assertThat(tenant).isNotNull();
    }

    @Test
    void whenTenantDisabledThenReturn() {
        var tenantDetailsProperties = new TenantDetailsProperties();
        tenantDetailsProperties.setTenants(List.of(buildTenantConfig("acme", false)));

        var tenantDetailsService = new PropertiesTenantDetailsService(tenantDetailsProperties);
        var tenant = tenantDetailsService.loadTenantByIdentifier("acme");

        assertThat(tenant).isNotNull();
    }

    private TenantDetailsProperties.TenantConfig buildTenantConfig(String identifier, boolean enabled) {
        var tenantConfig = new TenantDetailsProperties.TenantConfig();
        tenantConfig.setIdentifier(identifier);
        tenantConfig.setEnabled(enabled);
        return tenantConfig;
    }

}
