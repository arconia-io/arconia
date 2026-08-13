package io.arconia.multitenancy.core.actuate.endpoint;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import io.arconia.multitenancy.core.tenantdetails.Tenant;
import io.arconia.multitenancy.core.tenantdetails.TenantDetails;
import io.arconia.multitenancy.core.tenantdetails.TenantDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TenantsEndpoint}.
 */
class TenantsEndpointTests {

    @Test
    void whenNullTenantDetailsServiceThenThrow() {
        assertThatThrownBy(() -> new TenantsEndpoint(null)).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantDetailsService cannot be null");
    }

    @Test
    void whenNoTenantsThenEmptyList() {
        var endpoint = new TenantsEndpoint(new StubTenantDetailsService(List.of()));

        assertThat(endpoint.tenants().tenants()).isEmpty();
    }

    @Test
    void whenTenantsThenListedSortedByIdentifier() {
        var endpoint = new TenantsEndpoint(new StubTenantDetailsService(
                List.of(buildTenant("beans", true), buildTenant("acme", false))));

        assertThat(endpoint.tenants().tenants()).containsExactly(
                new TenantsEndpoint.TenantSummary("acme", false),
                new TenantsEndpoint.TenantSummary("beans", true));
    }

    @Test
    void whenTenantSelectedThenDescribed() {
        var endpoint = new TenantsEndpoint(new StubTenantDetailsService(List.of(buildTenant("acme", true))));

        var descriptor = endpoint.tenant("acme");

        assertThat(descriptor).isNotNull();
        assertThat(descriptor.identifier()).isEqualTo("acme");
        assertThat(descriptor.enabled()).isTrue();
    }

    @Test
    void whenTenantSelectedThenAttributeValuesAreNotExposed() {
        var tenant = Tenant.builder().identifier("acme").addAttribute("password", "s3cr3t").build();
        var endpoint = new TenantsEndpoint(new StubTenantDetailsService(List.of(tenant)));

        var descriptor = endpoint.tenant("acme");

        assertThat(descriptor).isNotNull();
        assertThat(descriptor.attributeNames()).containsExactly("password");
        assertThat(descriptor.toString()).doesNotContain("s3cr3t");
    }

    @Test
    void whenTenantUnknownThenNull() {
        var endpoint = new TenantsEndpoint(new StubTenantDetailsService(List.of()));

        assertThat(endpoint.tenant("unknown")).isNull();
    }

    private Tenant buildTenant(String identifier, boolean enabled) {
        return Tenant.builder().identifier(identifier).enabled(enabled).attributes(Map.of()).build();
    }

    private record StubTenantDetailsService(List<Tenant> tenants) implements TenantDetailsService {

        @Override
        public List<? extends TenantDetails> loadAllTenants() {
            return tenants;
        }

        @Override
        public @Nullable TenantDetails loadTenantByIdentifier(String identifier) {
            return tenants.stream().filter(tenant -> tenant.identifier().equals(identifier)).findFirst().orElse(null);
        }

    }

}
