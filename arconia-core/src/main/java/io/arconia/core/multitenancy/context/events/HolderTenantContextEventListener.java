package io.arconia.core.multitenancy.context.events;

import io.arconia.core.multitenancy.context.TenantContextHolder;
import io.arconia.core.multitenancy.events.TenantEvent;
import io.arconia.core.multitenancy.events.TenantEventListener;

/**
 * A {@link TenantEventListener} that sets/clears the tenant identifier from the current
 * context on the {@link TenantContextHolder}.
 *
 * @author Thomas Vitale
 */
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
