package io.arconia.multitenancy.core.tenantdetails;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.core.annotations.Incubating;

/**
 * An implementation of {@link TenantDetailsService} backed by a fixed list of tenants
 * held in memory, for example bound from application properties.
 * Identifiers must be unique.
 */
@Incubating
public final class InMemoryTenantDetailsService implements TenantDetailsService {

    private final List<? extends TenantDetails> tenants;

    private final Map<String, TenantDetails> tenantsByIdentifier;

    public InMemoryTenantDetailsService(List<? extends TenantDetails> tenants) {
        Assert.notNull(tenants, "tenants cannot be null");
        Assert.noNullElements(tenants, "tenants cannot contain null elements");
        var tenantsByIdentifier = new HashMap<String, TenantDetails>();
        var duplicates = new TreeSet<String>();
        for (var tenant : tenants) {
            if (tenantsByIdentifier.putIfAbsent(tenant.identifier(), tenant) != null) {
                duplicates.add(tenant.identifier());
            }
        }
        Assert.isTrue(duplicates.isEmpty(), () -> "tenants must have unique identifiers, but found duplicates: "
                + String.join(", ", duplicates));
        this.tenants = List.copyOf(tenants);
        this.tenantsByIdentifier = Map.copyOf(tenantsByIdentifier);
    }

    @Override
    public List<? extends TenantDetails> loadAllTenants() {
        return tenants;
    }

    @Nullable
    @Override
    public TenantDetails loadTenantByIdentifier(String identifier) {
        Assert.hasText(identifier, "identifier cannot be null or empty");
        return tenantsByIdentifier.get(identifier);
    }

}
