package io.arconia.multitenancy.core.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the tenant context.
 */
@ConfigurationProperties(prefix = TenantContextProperties.CONFIG_PREFIX)
public class TenantContextProperties {

    public static final String CONFIG_PREFIX = "arconia.multitenancy.context";

    /**
     * Tenant context propagation to the threads running asynchronous tasks.
     */
    private final Propagation propagation = new Propagation();

    public Propagation getPropagation() {
        return propagation;
    }

    public static class Propagation {

        /**
         * Whether the tenant context is propagated to the tasks run by the executors and
         * schedulers auto-configured by Spring Boot.
         */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

    }

}
