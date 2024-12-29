package io.arconia.multitenancy.core.context.events;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import io.arconia.multitenancy.core.events.TenantEvent;
import io.arconia.multitenancy.core.events.TenantEventListener;

/**
 * A {@link TenantEventListener} that sets the tenant identifier from the current context
 * on an existing {@link Observation}.
 */
public final class ObservationTenantContextEventListener implements TenantEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ObservationTenantContextEventListener.class);

    static final Cardinality DEFAULT_CARDINALITY = Cardinality.HIGH;

    static final String DEFAULT_TENANT_IDENTIFIER_KEY = "tenant.id";

    private final Cardinality cardinality;

    private final String tenantIdentifierKey;

    public ObservationTenantContextEventListener() {
        this(DEFAULT_TENANT_IDENTIFIER_KEY, DEFAULT_CARDINALITY);
    }

    public ObservationTenantContextEventListener(String tenantIdentifierKey, Cardinality cardinality) {
        Assert.hasText(tenantIdentifierKey, "tenantIdentifierKey cannot be null or empty");
        Assert.notNull(cardinality, "cardinality cannot be null");
        this.tenantIdentifierKey = tenantIdentifierKey;
        this.cardinality = cardinality;
    }

    @Override
    public void onApplicationEvent(TenantEvent tenantEvent) {
        if (tenantEvent instanceof TenantContextAttachedEvent event) {
            onTenantContextAttached(event);
        }
    }

    private void onTenantContextAttached(TenantContextAttachedEvent event) {
        if (event.getObservationContext() == null) {
            return;
        }

        logger.trace("Enhancing current observation with tenant context for: {}", event.getTenantIdentifier());

        switch (cardinality) {
            case LOW -> event.getObservationContext()
                .addLowCardinalityKeyValue(KeyValue.of(tenantIdentifierKey, event.getTenantIdentifier()));
            case HIGH -> event.getObservationContext()
                .addHighCardinalityKeyValue(KeyValue.of(tenantIdentifierKey, event.getTenantIdentifier()));
        }
    }

    public enum Cardinality {

        LOW, HIGH

    }

}
