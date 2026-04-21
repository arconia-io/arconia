package io.arconia.multitenancy.core.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.arconia.multitenancy.core.observability.Cardinality;

/**
 * Configuration properties for tenant observation enrichment.
 */
@ConfigurationProperties(prefix = TenantObservationProperties.CONFIG_PREFIX)
public class TenantObservationProperties {

    public static final String CONFIG_PREFIX = "arconia.multitenancy.observations";

    /**
     * Whether observations are enhanced with tenant information.
     */
    private boolean enabled = true;

    /**
     * Name of the key to use for the tenant identifier in observations.
     */
    private String keyName = "tenant.id";

    /**
     * The cardinality of the tenant identifier key value. {@code HIGH} (default) adds it
     * as a high-cardinality key value, appearing only in traces. {@code LOW} adds it as a
     * low-cardinality key value, appearing in both metrics and traces.
     */
    private Cardinality cardinality = Cardinality.HIGH;

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

    public Cardinality getCardinality() {
        return cardinality;
    }

    public void setCardinality(Cardinality cardinality) {
        this.cardinality = cardinality;
    }

}
