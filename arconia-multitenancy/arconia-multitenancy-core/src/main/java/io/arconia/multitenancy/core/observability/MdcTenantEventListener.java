package io.arconia.multitenancy.core.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.event.EventListener;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;
import io.arconia.multitenancy.core.context.events.TenantContextAttachedEvent;
import io.arconia.multitenancy.core.context.events.TenantContextClosedEvent;

/**
 * Manages the SLF4J {@link MDC} tenant identifier in response to tenant context events.
 */
@Incubating
public final class MdcTenantEventListener {

    private static final Logger logger = LoggerFactory.getLogger(MdcTenantEventListener.class);

    private static final String DEFAULT_TENANT_IDENTIFIER_KEY = "tenantId";

    private final String tenantIdentifierKey;

    public MdcTenantEventListener() {
        this(DEFAULT_TENANT_IDENTIFIER_KEY);
    }

    public MdcTenantEventListener(String tenantIdentifierKey) {
        Assert.hasText(tenantIdentifierKey, "tenantIdentifierKey cannot be null or empty");
        this.tenantIdentifierKey = tenantIdentifierKey;
    }

    @EventListener
    void onAttached(TenantContextAttachedEvent event) {
        logger.trace("Setting current tenant in MDC to: {}", event.getTenantIdentifier());
        MDC.put(tenantIdentifierKey, event.getTenantIdentifier());
    }

    @EventListener
    void onClosed(TenantContextClosedEvent event) {
        logger.trace("Removing current tenant from MDC");
        MDC.remove(tenantIdentifierKey);
    }

}
