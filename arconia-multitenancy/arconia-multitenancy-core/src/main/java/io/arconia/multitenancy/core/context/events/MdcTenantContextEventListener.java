package io.arconia.multitenancy.core.context.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.util.Assert;

import io.arconia.multitenancy.core.events.TenantEvent;
import io.arconia.multitenancy.core.events.TenantEventListener;

/**
 * A {@link TenantEventListener} that sets/clears the tenant identifier from the current
 * context on the SLF4J's {@link MDC}.
 */
public final class MdcTenantContextEventListener implements TenantEventListener {

    private static final Logger logger = LoggerFactory.getLogger(MdcTenantContextEventListener.class);

    private static final String DEFAULT_TENANT_IDENTIFIER_KEY = "tenantId";

    private final String tenantIdentifierKey;

    public MdcTenantContextEventListener() {
        this(DEFAULT_TENANT_IDENTIFIER_KEY);
    }

    public MdcTenantContextEventListener(String tenantIdentifierKey) {
        Assert.hasText(tenantIdentifierKey, "tenantIdentifierKey cannot be null or empty");
        this.tenantIdentifierKey = tenantIdentifierKey;
    }

    @Override
    public void onApplicationEvent(TenantEvent tenantEvent) {
        if (tenantEvent instanceof TenantContextAttachedEvent event) {
            logger.trace("Setting current tenant in MDC to: {}", event.getTenantIdentifier());
            MDC.put(tenantIdentifierKey, event.getTenantIdentifier());
        }
        else if (tenantEvent instanceof TenantContextClosedEvent) {
            logger.trace("Removing current tenant from MDC to");
            MDC.remove(tenantIdentifierKey);
        }
    }

}
