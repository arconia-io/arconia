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

    public static final String DEFAULT_TENANT_ID_KEY = "tenantId";

    private final String tenantIdKey;

    public MdcTenantContextEventListener() {
        this(DEFAULT_TENANT_ID_KEY);
    }

    public MdcTenantContextEventListener(String tenantIdKey) {
        Assert.hasText(tenantIdKey, "tenantIdKey cannot be empty");
        this.tenantIdKey = tenantIdKey;
    }

    @Override
    public void onApplicationEvent(TenantEvent tenantEvent) {
        if (tenantEvent instanceof TenantContextAttachedEvent event) {
            MDC.put(tenantIdKey, event.getTenantId());
        }
        else if (tenantEvent instanceof TenantContextClosedEvent) {
            MDC.remove(tenantIdKey);
        }
    }

}
