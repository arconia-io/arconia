package io.arconia.multitenancy.core.tenantdetails;

import java.util.Map;

import io.arconia.core.support.Incubating;

/**
 * Provides core tenant information.
 */
@Incubating
public interface TenantDetails {

    /**
     * Identifier for the tenant.
     */
    String identifier();

    /**
     * Whether the tenant is enabled.
     */
    boolean enabled();

    /**
     * Additional information about the tenant.
     */
    default Map<String, Object> attributes() {
        return Map.of();
    }

}
