package io.arconia.multitenancy.core.context.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.arconia.multitenancy.core.events.TenantEvent;
import io.arconia.multitenancy.core.events.TenantEventListener;
import io.arconia.multitenancy.core.exceptions.TenantResolutionException;
import io.arconia.multitenancy.core.tenantdetails.TenantDetailsService;

/**
 * A {@link TenantEventListener} that validates the tenant for the current context.
 */
public final class ValidatingTenantContextEventListener implements TenantEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ValidatingTenantContextEventListener.class);

    private final TenantDetailsService tenantDetailsService;

    public ValidatingTenantContextEventListener(TenantDetailsService tenantDetailsService) {
        this.tenantDetailsService = tenantDetailsService;
    }

    @Override
    public void onApplicationEvent(TenantEvent tenantEvent) {
        if (tenantEvent instanceof TenantContextAttachedEvent event) {
            logger.trace("Validating tenant {}", event.getTenantIdentifier());
            var tenant = tenantDetailsService.loadTenantByIdentifier(event.getTenantIdentifier());
            if (tenant == null || !tenant.isEnabled()) {
                throw new TenantResolutionException("The resolved tenant is invalid or disabled");
            }
        }
    }

}
