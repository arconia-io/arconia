package io.arconia.multitenancy.core.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.arconia.multitenancy.core.context.events.ObservationTenantContextEventListener;

/**
 * Configuration properties for tenant management.
 */
@ConfigurationProperties(prefix = TenantManagementProperties.CONFIG_PREFIX)
public class TenantManagementProperties {

    public static final String CONFIG_PREFIX = "arconia.multitenancy.management";

    /**
     * Tenant configuration for MDC.
     */
    private final Mdc mdc = new Mdc();

    /**
     * Tenant configuration for observations.
     */
    private final Observations observations = new Observations();

    public Mdc getMdc() {
        return mdc;
    }

    public Observations getObservations() {
        return observations;
    }

    public static class Mdc {

        /**
         * Whether to include tenant information in MDC.
         */
        private boolean enabled = true;

        /**
         * Key to use for including the tenant identifier information in MDC.
         */
        private String key = "tenantId";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

    }

    public static class Observations {

        /**
         * Whether observations are enhanced with tenant information.
         */
        private boolean enabled = true;

        /**
         * Key to use for including the tenant identifier information in observations.
         */
        private String key = "tenant.id";

        /**
         * Whether to include the tenant identifier information in traces ('high'
         * cardinality) or also in metrics ('low' cardinality).
         */
        private ObservationTenantContextEventListener.Cardinality cardinality = ObservationTenantContextEventListener.Cardinality.HIGH;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public ObservationTenantContextEventListener.Cardinality getCardinality() {
            return cardinality;
        }

        public void setCardinality(ObservationTenantContextEventListener.Cardinality cardinality) {
            this.cardinality = cardinality;
        }

    }

}
