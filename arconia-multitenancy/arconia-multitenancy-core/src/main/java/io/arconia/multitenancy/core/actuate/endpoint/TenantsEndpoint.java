package io.arconia.multitenancy.core.actuate.endpoint;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;
import io.arconia.multitenancy.core.tenantdetails.TenantDetails;
import io.arconia.multitenancy.core.tenantdetails.TenantDetailsService;

/**
 * Endpoint for exposing the tenants known to the application.
 * <p>
 * Attribute values are never exposed, only attribute names, since tenant attributes are
 * application-defined and may carry credentials.
 */
@Incubating
@Endpoint(id = "tenants")
public class TenantsEndpoint {

    private final TenantDetailsService tenantDetailsService;

    public TenantsEndpoint(TenantDetailsService tenantDetailsService) {
        Assert.notNull(tenantDetailsService, "tenantDetailsService cannot be null");
        this.tenantDetailsService = tenantDetailsService;
    }

    @ReadOperation
    public TenantsDescriptor tenants() {
        List<TenantSummary> tenants = tenantDetailsService.loadAllTenants()
            .stream()
            .map(tenant -> new TenantSummary(tenant.identifier(), tenant.enabled()))
            .sorted(Comparator.comparing(TenantSummary::identifier))
            .collect(Collectors.toList());
        return new TenantsDescriptor(tenants);
    }

    @ReadOperation
    @Nullable
    public TenantDescriptor tenant(@Selector String identifier) {
        TenantDetails tenant = tenantDetailsService.loadTenantByIdentifier(identifier);
        if (tenant == null) {
            // A null result is mapped to a 404 response.
            return null;
        }
        return new TenantDescriptor(tenant.identifier(), tenant.enabled(), new TreeSet<>(tenant.attributes().keySet()));
    }

    public record TenantsDescriptor(List<TenantSummary> tenants) {}

    public record TenantSummary(String identifier, boolean enabled) {}

    public record TenantDescriptor(String identifier, boolean enabled, Set<String> attributeNames) {}

}
