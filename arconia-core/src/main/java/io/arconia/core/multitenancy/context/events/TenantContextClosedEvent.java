package io.arconia.core.multitenancy.context.events;

import io.arconia.core.multitenancy.events.TenantEvent;

/**
 * A {@link TenantEvent} which indicates the context for the current tenant has been
 * closed.
 *
 * @author Thomas Vitale
 */
public final class TenantContextClosedEvent extends TenantEvent {

    public TenantContextClosedEvent(String tenantIdentifier, Object object) {
        super(tenantIdentifier, object);
    }

}
