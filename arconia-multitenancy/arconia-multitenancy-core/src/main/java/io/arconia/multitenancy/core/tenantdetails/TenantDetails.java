package io.arconia.multitenancy.core.tenantdetails;

import java.util.Map;

/**
 * Provides core tenant information.
 */
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
