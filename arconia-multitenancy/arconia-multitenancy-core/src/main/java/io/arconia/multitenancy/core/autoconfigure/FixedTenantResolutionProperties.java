package io.arconia.multitenancy.core.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for fixed tenant resolution.
 */
@ConfigurationProperties(prefix = FixedTenantResolutionProperties.CONFIG_PREFIX)
public class FixedTenantResolutionProperties {

    public static final String CONFIG_PREFIX = "arconia.multitenancy.resolution.fixed";

    /**
     * Identifier of the fixed tenant to use when the fixed resolution strategy is part
     * of a resolution chain.
     */
    private String tenantIdentifier = "default";

    public String getTenantIdentifier() {
        return tenantIdentifier;
    }

    public void setTenantIdentifier(String tenantIdentifier) {
        this.tenantIdentifier = tenantIdentifier;
    }

}
