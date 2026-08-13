package io.arconia.multitenancy.core.tenantdetails;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TenantDetailsManager}, exercising the contract through a minimal
 * in-memory implementation.
 */
class TenantDetailsManagerTests {

    private final TenantDetailsManager tenantDetailsManager = new InMemoryTenantDetailsManager();

    @Test
    void whenTenantCreatedThenReturned() {
        var tenant = tenantDetailsManager.createTenant("acme", true, Map.of("region", "eu"));

        assertThat(tenant.identifier()).isEqualTo("acme");
        assertThat(tenant.enabled()).isTrue();
        assertThat(tenant.attributes()).containsExactly(Map.entry("region", "eu"));
    }

    @Test
    void whenTenantCreatedThenLoadableByIdentifier() {
        tenantDetailsManager.createTenant("acme", true, Map.of());

        assertThat(tenantDetailsManager.loadTenantByIdentifier("acme")).isNotNull();
    }

    @Test
    void whenTenantNotCreatedThenNotLoadableByIdentifier() {
        assertThat(tenantDetailsManager.loadTenantByIdentifier("unknown")).isNull();
    }

    @Test
    void whenTenantsCreatedThenAllLoadable() {
        tenantDetailsManager.createTenant("acme", true, Map.of());
        tenantDetailsManager.createTenant("beans", false, Map.of());

        assertThat(tenantDetailsManager.loadAllTenants()).hasSize(2);
    }

    private static final class InMemoryTenantDetailsManager implements TenantDetailsManager {

        private final Map<String, TenantDetails> tenants = new ConcurrentHashMap<>();

        @Override
        public TenantDetails createTenant(String identifier, boolean enabled, Map<String, Object> attributes) {
            var tenant = Tenant.builder().identifier(identifier).enabled(enabled).attributes(attributes).build();
            tenants.put(identifier, tenant);
            return tenant;
        }

        @Override
        public List<? extends TenantDetails> loadAllTenants() {
            return List.copyOf(tenants.values());
        }

        @Override
        public @Nullable TenantDetails loadTenantByIdentifier(String identifier) {
            return tenants.get(identifier);
        }

    }

}
