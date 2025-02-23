package io.arconia.multitenancy.core.context.events;

import io.micrometer.observation.Observation.Context;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.multitenancy.core.events.TenantEvent;

/**
 * A {@link TenantEvent} which indicates a tenant has been attached to the current
 * context.
 */
public final class TenantContextAttachedEvent extends TenantEvent {

    @Nullable
    private Context observationContext;

    public TenantContextAttachedEvent(String tenantIdentifier, Object object) {
        super(tenantIdentifier, object);
    }

    @Nullable
    public Context getObservationContext() {
        return observationContext;
    }

    public void setObservationContext(Context observationContext) {
        Assert.notNull(observationContext, "observationContext cannot be null");
        this.observationContext = observationContext;
    }

}
