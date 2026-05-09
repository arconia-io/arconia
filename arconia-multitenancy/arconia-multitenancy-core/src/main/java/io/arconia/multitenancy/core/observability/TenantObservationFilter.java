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

    static final String DEFAULT_TENANT_IDENTIFIER_KEY = "tenant.id";

    private final String tenantIdentifierKey;

    private final Cardinality cardinality;

    public TenantObservationFilter() {
        this(DEFAULT_TENANT_IDENTIFIER_KEY, Cardinality.HIGH);
    }

    public TenantObservationFilter(String tenantIdentifierKey, Cardinality cardinality) {
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

}
