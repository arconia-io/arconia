package io.arconia.multitenancy.core.observability;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;
import io.arconia.multitenancy.core.context.TenantContext;

/**
 * An {@link ObservationFilter} that enriches all observations with the current tenant
 * identifier from the {@link TenantContext}.
 */
@Incubating
public final class TenantObservationFilter implements ObservationFilter {

    private static final Logger logger = LoggerFactory.getLogger(TenantObservationFilter.class);

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

    @Override
    public Observation.Context map(Observation.Context context) {
        var tenantIdentifier = TenantContext.getTenantIdentifier();
        if (tenantIdentifier == null) {
            return context;
        }

        logger.trace("Enhancing observation with tenant context for: {}", tenantIdentifier);

        var keyValue = KeyValue.of(tenantIdentifierKey, tenantIdentifier);
        if (cardinality == Cardinality.LOW) {
            context.addLowCardinalityKeyValue(keyValue);
        }
        else {
            context.addHighCardinalityKeyValue(keyValue);
        }

        return context;
    }

}
