package io.arconia.multitenancy.core.context.events;

import io.arconia.core.support.Incubating;
import io.arconia.multitenancy.core.events.TenantEvent;

/**
 * A {@link TenantEvent} which indicates the context for the current tenant has been
 * closed.
 */
@Incubating(since = "0.1.0")
public final class TenantContextClosedEvent extends TenantEvent {

    public TenantContextClosedEvent(String tenantIdentifier, Object object) {
        super(tenantIdentifier, object);
    }

}
