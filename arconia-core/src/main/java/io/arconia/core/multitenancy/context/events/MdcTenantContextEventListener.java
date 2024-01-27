package io.arconia.core.multitenancy.context.events;

import org.slf4j.MDC;
import org.springframework.util.Assert;

import io.arconia.core.multitenancy.events.TenantEvent;
import io.arconia.core.multitenancy.events.TenantEventListener;

/**
 * A {@link TenantEventListener} that sets/clears the tenant identifier from the current
 * context on the SLF4J's {@link MDC}.
 *
 * @author Thomas Vitale
 */
public final class MdcTenantContextEventListener implements TenantEventListener {

    public static final String DEFAULT_TENANT_IDENTIFIER_KEY = "tenantId";

    private final String tenantIdentifierKey;

    public MdcTenantContextEventListener() {
        this(DEFAULT_TENANT_IDENTIFIER_KEY);
    }

    public MdcTenantContextEventListener(String tenantIdentifierKey) {
        Assert.hasText(tenantIdentifierKey, "tenantIdentifierKey cannot be empty");
        this.tenantIdentifierKey = tenantIdentifierKey;
    }

    @Override
    public void onApplicationEvent(TenantEvent tenantEvent) {
        if (tenantEvent instanceof TenantContextAttachedEvent event) {
            MDC.put(tenantIdentifierKey, event.getTenantIdentifier());
        }
        else if (tenantEvent instanceof TenantContextClosedEvent) {
            MDC.remove(tenantIdentifierKey);
        }
    }

}
