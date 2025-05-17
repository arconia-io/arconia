package io.arconia.multitenancy.core.context.events;

import io.arconia.core.support.Incubating;
import io.arconia.multitenancy.core.context.TenantContextHolder;
import io.arconia.multitenancy.core.events.TenantEvent;
import io.arconia.multitenancy.core.events.TenantEventListener;

/**
 * A {@link TenantEventListener} that sets/clears the tenant identifier from the current
 * context on the {@link TenantContextHolder}.
 */
@Incubating
public final class HolderTenantContextEventListener implements TenantEventListener {

    @Override
    public void onApplicationEvent(TenantEvent tenantEvent) {
        if (tenantEvent instanceof TenantContextAttachedEvent event) {
            TenantContextHolder.setTenantIdentifier(event.getTenantIdentifier());
        }
        else if (tenantEvent instanceof TenantContextClosedEvent) {
            TenantContextHolder.clear();
        }
    }

}
