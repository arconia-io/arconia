package io.arconia.autoconfigure.multitenancy.core.tenantdetails;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.arconia.core.multitenancy.tenantdetails.Tenant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PropertiesTenantDetailsService}.
 */
class PropertiesTenantDetailsServiceTests {

    @Test
    void loadAllTenants() {
        var tenantDetailsProperties = new TenantDetailsProperties();
        tenantDetailsProperties.setTenants(List.of(Tenant.create().identifier("acme").enabled(true).build(),
                Tenant.create().identifier("sam").enabled(false).build()));

        var tenantDetailsService = new PropertiesTenantDetailsService(tenantDetailsProperties);
        var tenants = tenantDetailsService.loadAllTenants();

        assertThat(tenants).isNotNull();
        assertThat(tenants).hasSize(2);
    }

    @Test
    void whenTenantEnabledThenReturn() {
        var tenantDetailsProperties = new TenantDetailsProperties();
        tenantDetailsProperties.setTenants(List.of(Tenant.create().identifier("acme").enabled(true).build()));

        var tenantDetailsService = new PropertiesTenantDetailsService(tenantDetailsProperties);
        var tenant = tenantDetailsService.loadTenantByIdentifier("acme");

        assertThat(tenant).isNotNull();
    }

    @Test
    void whenTenantDisabledThenReturn() {
        var tenantDetailsProperties = new TenantDetailsProperties();
        tenantDetailsProperties.setTenants(List.of(Tenant.create().identifier("acme").build()));

        var tenantDetailsService = new PropertiesTenantDetailsService(tenantDetailsProperties);
        var tenant = tenantDetailsService.loadTenantByIdentifier("acme");

        assertThat(tenant).isNotNull();
    }

}
