package io.arconia.multitenancy.core.tenantdetails;

import java.util.Map;

import io.arconia.core.support.Incubating;

/**
 * A {@link TenantDetailsService} that can also create tenants, for scenarios where the
 * set of tenants is managed at runtime rather than fixed at deployment time.
 */
@Incubating
public interface TenantDetailsManager extends TenantDetailsService {

    /**
     * Creates a tenant with the given identifier, state and additional information, and
     * returns the tenant that was created.
     */
    TenantDetails createTenant(String identifier, boolean enabled, Map<String, Object> attributes);

}
