package io.arconia.core.multitenancy.context.events;

import io.micrometer.observation.Observation;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import io.arconia.core.multitenancy.events.TenantEvent;

/**
 * A {@link TenantEvent} which indicates a tenant has been attached to the current
 * context.
 *
 * @author Thomas Vitale
 */
public final class TenantContextAttachedEvent extends TenantEvent {

    @Nullable
    private Observation.Context observationContext;

    public TenantContextAttachedEvent(String tenantIdentifier, Object object) {
        super(tenantIdentifier, object);
    }

    @Nullable
    public Observation.Context getObservationContext() {
        return observationContext;
    }

    public void setObservationContext(Observation.Context observationContext) {
        Assert.notNull(observationContext, "observationContext cannot be null");
        this.observationContext = observationContext;
    }

}
