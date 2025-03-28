package io.arconia.multitenancy.autoconfigure.core;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for fixed tenant resolution.
 */
@ConfigurationProperties(prefix = FixedTenantResolutionProperties.CONFIG_PREFIX)
public class FixedTenantResolutionProperties {

    public static final String CONFIG_PREFIX = "arconia.multitenancy.resolution.fixed";

    /**
     * Whether a fixed tenant resolution strategy should be used.
     */
    private boolean enabled = false;

    /**
     * Identifier of the fixed tenant to use in each context.
     */
    private String tenantIdentifier = "default";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTenantIdentifier() {
        return tenantIdentifier;
    }

    public void setTenantIdentifier(String tenantIdentifier) {
        this.tenantIdentifier = tenantIdentifier;
    }

}
