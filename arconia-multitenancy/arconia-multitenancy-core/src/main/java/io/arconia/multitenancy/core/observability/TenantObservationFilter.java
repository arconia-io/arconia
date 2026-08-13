package io.arconia.multitenancy.core.observability;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationFilter;

import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;
import io.arconia.multitenancy.core.context.TenantContext;

/**
 * An {@link ObservationFilter} that enriches all observations with the current tenant
 * identifier from the {@link TenantContext}.
 */
@Incubating
public final class TenantObservationFilter implements ObservationFilter {

    private static final String DEFAULT_TENANT_IDENTIFIER_KEY = "tenant.id";

    private static final Cardinality DEFAULT_CARDINALITY = Cardinality.HIGH;

    private final String tenantIdentifierKey;

    private final Cardinality cardinality;

    private TenantObservationFilter(String tenantIdentifierKey, Cardinality cardinality) {
        Assert.hasText(tenantIdentifierKey, "tenantIdentifierKey cannot be null or empty");
        Assert.notNull(cardinality, "cardinality cannot be null");
        this.tenantIdentifierKey = tenantIdentifierKey;
        this.cardinality = cardinality;
    }

    public String getTenantIdentifierKey() {
        return tenantIdentifierKey;
    }

    public Cardinality getCardinality() {
        return cardinality;
    }

    @Override
    public Observation.Context map(Observation.Context context) {
        var tenantIdentifier = TenantContext.getTenantIdentifier();
        if (tenantIdentifier == null) {
            return context;
        }

        var keyValue = KeyValue.of(tenantIdentifierKey, tenantIdentifier);
        if (cardinality == Cardinality.LOW) {
            context.addLowCardinalityKeyValue(keyValue);
        } else {
            context.addHighCardinalityKeyValue(keyValue);
        }

        return context;
    }

    /**
     * Creates a new builder for {@link TenantObservationFilter}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link TenantObservationFilter}.
     */
    public static final class Builder {

        private String tenantIdentifierKey = DEFAULT_TENANT_IDENTIFIER_KEY;

        private Cardinality cardinality = DEFAULT_CARDINALITY;

        private Builder() {}

        /**
         * Name of the key to use for the tenant identifier in observations.
         */
        public Builder tenantIdentifierKey(String tenantIdentifierKey) {
            this.tenantIdentifierKey = tenantIdentifierKey;
            return this;
        }

        /**
         * The cardinality of the tenant identifier key value.
         */
        public Builder cardinality(Cardinality cardinality) {
            this.cardinality = cardinality;
            return this;
        }

        /**
         * Builds the {@link TenantObservationFilter} instance.
         */
        public TenantObservationFilter build() {
            return new TenantObservationFilter(tenantIdentifierKey, cardinality);
        }

    }

}
