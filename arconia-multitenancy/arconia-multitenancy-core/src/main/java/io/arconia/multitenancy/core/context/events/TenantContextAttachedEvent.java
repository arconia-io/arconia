package io.arconia.multitenancy.core.context.events;

import io.arconia.core.support.Incubating;

/**
 * A {@link TenantEvent} which indicates a tenant has been attached to the current
 * context.
 */
@Incubating
public final class TenantContextAttachedEvent extends TenantEvent {

    public TenantContextAttachedEvent(String tenantIdentifier, Object object) {
        super(tenantIdentifier, object);
    }

}
