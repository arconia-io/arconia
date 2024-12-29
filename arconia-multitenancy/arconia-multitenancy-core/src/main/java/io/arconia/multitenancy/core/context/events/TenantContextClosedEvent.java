package io.arconia.multitenancy.core.context.events;

import io.arconia.multitenancy.core.events.TenantEvent;

/**
 * A {@link TenantEvent} which indicates the context for the current tenant has been
 * closed.
 */
public final class TenantContextClosedEvent extends TenantEvent {

    public TenantContextClosedEvent(String tenantIdentifier, Object object) {
        super(tenantIdentifier, object);
    }

}
