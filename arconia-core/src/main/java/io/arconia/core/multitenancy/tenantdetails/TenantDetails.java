package io.arconia.core.multitenancy.tenantdetails;

import java.io.Serializable;
import java.util.Map;

/**
 * Provides core tenant information.
 */
public interface TenantDetails extends Serializable {

    /**
     * Identifier for the tenant.
     */
    String getIdentifier();

    /**
     * Whether the tenant is enabled.
     */
    boolean isEnabled();

    /**
     * Additional information about the tenant.
     */
    default Map<String, Object> getAttributes() {
        return Map.of();
    }

}
