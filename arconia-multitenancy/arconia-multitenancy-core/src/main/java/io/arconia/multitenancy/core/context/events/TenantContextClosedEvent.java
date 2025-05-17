package io.arconia.multitenancy.core.context.events;

import io.arconia.core.support.Incubating;
import io.arconia.multitenancy.core.events.TenantEvent;

/**
 * A {@link TenantEvent} which indicates the context for the current tenant has been
 * closed.
 */
@Incubating
public final class TenantContextClosedEvent extends TenantEvent {

    public TenantContextClosedEvent(String tenantIdentifier, Object object) {
        super(tenantIdentifier, object);
    }

}
