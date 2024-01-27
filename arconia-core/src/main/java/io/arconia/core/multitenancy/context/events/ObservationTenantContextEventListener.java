package io.arconia.core.multitenancy.context.events;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;

import org.springframework.util.Assert;

import io.arconia.core.multitenancy.events.TenantEvent;
import io.arconia.core.multitenancy.events.TenantEventListener;

/**
 * A {@link TenantEventListener} that sets the tenant identifier from the current context
 * on an existing {@link Observation}.
 *
 * @author Thomas Vitale
 */
public class ObservationTenantContextEventListener implements TenantEventListener {

    public static final Cardinality DEFAULT_CARDINALITY = Cardinality.HIGH;

    public static final String DEFAULT_TENANT_ID_KEY = "tenant.id";

    private final Cardinality cardinality;

    private final String tenantIdKey;

    public ObservationTenantContextEventListener() {
        this(DEFAULT_TENANT_ID_KEY, DEFAULT_CARDINALITY);
    }

    public ObservationTenantContextEventListener(String tenantIdKey, Cardinality cardinality) {
        Assert.hasText(tenantIdKey, "tenantIdKey cannot be empty");
        Assert.notNull(cardinality, "cardinality cannot be null");
        this.tenantIdKey = tenantIdKey;
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

        switch (cardinality) {
            case LOW ->
                event.getObservationContext().addLowCardinalityKeyValue(KeyValue.of(tenantIdKey, event.getTenantId()));
            case HIGH ->
                event.getObservationContext().addHighCardinalityKeyValue(KeyValue.of(tenantIdKey, event.getTenantId()));
        }
    }

    public enum Cardinality {

        LOW, HIGH

    }

}
