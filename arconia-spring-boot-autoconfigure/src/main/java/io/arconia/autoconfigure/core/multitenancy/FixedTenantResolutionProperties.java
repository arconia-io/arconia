package io.arconia.autoconfigure.core.multitenancy;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.arconia.core.multitenancy.context.resolvers.FixedTenantResolver;

/**
 * Configuration properties for fixed tenant resolution.
 *
 * @author Thomas Vitale
 */
@ConfigurationProperties(prefix = FixedTenantResolutionProperties.CONFIG_PREFIX)
public class FixedTenantResolutionProperties {

    public static final String CONFIG_PREFIX = "arconia.multitenancy.resolution.fixed";

    /**
     * Whether a fixed tenant resolution strategy should be used.
     */
    private boolean enabled = false;

    /**
     * The name of the fixed tenant to use in each context.
     */
    private String tenantId = FixedTenantResolver.DEFAULT_FIXED_TENANT;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

}
