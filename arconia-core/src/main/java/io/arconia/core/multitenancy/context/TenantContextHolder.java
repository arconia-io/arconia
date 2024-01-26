package io.arconia.core.multitenancy.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import io.arconia.core.multitenancy.exceptions.TenantNotFoundException;

/**
 * A shared, thread-local store for the current tenant.
 *
 * @author Thomas Vitale
 */
public final class TenantContextHolder {

    private static final Logger log = LoggerFactory.getLogger(TenantContextHolder.class);

    private static final ThreadLocal<String> tenantId = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void setTenantId(final String tenant) {
        Assert.hasText(tenant, "tenantId cannot be empty");
        log.trace("Setting current tenant to: {}", tenant);
        tenantId.set(tenant);
    }

    @Nullable
    public static String getTenantId() {
        return tenantId.get();
    }

    public static String getRequiredTenantId() {
        var tenantId = getTenantId();
        if (!StringUtils.hasText(tenantId)) {
            throw new TenantNotFoundException("No tenant found in the current context");
        }
        return tenantId;
    }

    public static void clear() {
        log.trace("Clearing current tenant");
        tenantId.remove();
    }

}
