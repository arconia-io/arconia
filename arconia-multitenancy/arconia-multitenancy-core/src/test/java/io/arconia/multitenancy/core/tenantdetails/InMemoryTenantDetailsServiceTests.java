package io.arconia.multitenancy.core.tenantdetails;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link InMemoryTenantDetailsService}.
 */
class InMemoryTenantDetailsServiceTests {

    @Test
    void whenNullTenantsThenThrow() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new InMemoryTenantDetailsService(null))
            .withMessageContaining("tenants cannot be null");
    }

    @Test
    void whenNullIdentifierThenThrow() {
        var tenantDetailsService = new InMemoryTenantDetailsService(List.of());

        assertThatIllegalArgumentException()
            .isThrownBy(() -> tenantDetailsService.loadTenantByIdentifier(null))
            .withMessageContaining("identifier cannot be null or empty");
    }

    @Test
    void loadAllTenants() {
        var tenantDetailsService = new InMemoryTenantDetailsService(List.of(tenant("acme", true), tenant("sam", false)));

        assertThat(tenantDetailsService.loadAllTenants()).hasSize(2);
    }

    @Test
    void whenTenantEnabledThenReturn() {
        var tenantDetailsService = new InMemoryTenantDetailsService(List.of(tenant("acme", true)));

        assertThat(tenantDetailsService.loadTenantByIdentifier("acme")).isNotNull();
    }

    @Test
    void whenTenantDisabledThenReturn() {
        var tenantDetailsService = new InMemoryTenantDetailsService(List.of(tenant("acme", false)));

        assertThat(tenantDetailsService.loadTenantByIdentifier("acme")).isNotNull();
    }

    @Test
    void whenTenantUnknownThenNull() {
        var tenantDetailsService = new InMemoryTenantDetailsService(List.of(tenant("acme", true)));

        assertThat(tenantDetailsService.loadTenantByIdentifier("unknown")).isNull();
    }

    @Test
    void whenTenantLoadedThenCarriesConfiguredState() {
        var tenantDetailsService = new InMemoryTenantDetailsService(List.of(tenant("acme", true), tenant("beans", false)));

        var tenant = tenantDetailsService.loadTenantByIdentifier("beans");

        assertThat(tenant).isNotNull();
        assertThat(tenant.identifier()).isEqualTo("beans");
        assertThat(tenant.enabled()).isFalse();
    }

    @Test
    void whenSourceListChangesThenServiceIsUnaffected() {
        var tenants = new java.util.ArrayList<>(List.of(tenant("acme", true)));
        var tenantDetailsService = new InMemoryTenantDetailsService(tenants);
        tenants.clear();

        assertThat(tenantDetailsService.loadAllTenants()).hasSize(1);
    }

    private static Tenant tenant(String identifier, boolean enabled) {
        return Tenant.builder().identifier(identifier).enabled(enabled).build();
    }

}
