package io.arconia.multitenancy.core.tenantdetails;

import java.util.Map;

import org.jspecify.annotations.Nullable;

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

    /**
     * Returns an attribute value by key and type.
     */
    @Nullable
    default <T> T attribute(String key, Class<T> type) {
        try {
            return type.cast(attributes().get(key));
        } catch (ClassCastException e) {
            throw new IllegalStateException(String.format("Attribute '%s' is not of type '%s'", key, type.getName()), e);
        }
    }

}
