package io.arconia.multitenancy.core.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for tenant logging enrichment.
 */
@ConfigurationProperties(prefix = TenantLoggingProperties.CONFIG_PREFIX)
public class TenantLoggingProperties {

    public static final String CONFIG_PREFIX = "arconia.multitenancy.logging";

    /**
     * Tenant configuration for MDC.
     */
    private final Mdc mdc = new Mdc();

    public Mdc getMdc() {
        return mdc;
    }

    public static class Mdc {

        /**
         * Whether to include tenant information in MDC.
         */
        private boolean enabled = true;

        /**
         * Name of the key to use for the tenant identifier in MDC.
         */
        private String keyName = "tenantId";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getKeyName() {
            return keyName;
        }

        public void setKeyName(String keyName) {
            this.keyName = keyName;
        }

    }

}
