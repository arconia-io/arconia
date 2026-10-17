package io.arconia.multitenancy.core.tenantdetails;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;

/**
 * An implementation of {@link TenantDetailsService} backed by a fixed list of tenants
 * held in memory, for example bound from application properties.
 */
@Incubating
public final class InMemoryTenantDetailsService implements TenantDetailsService {

    private final List<? extends TenantDetails> tenants;

    public InMemoryTenantDetailsService(List<? extends TenantDetails> tenants) {
        Assert.notNull(tenants, "tenants cannot be null");
        Assert.noNullElements(tenants, "tenants cannot contain null elements");
        this.tenants = List.copyOf(tenants);
    }

    @Override
    public List<? extends TenantDetails> loadAllTenants() {
        return tenants;
    }

    @Nullable
    @Override
    public TenantDetails loadTenantByIdentifier(String identifier) {
        Assert.hasText(identifier, "identifier cannot be null or empty");
        return tenants.stream().filter(tenant -> identifier.equals(tenant.identifier())).findFirst().orElse(null);
    }

}
