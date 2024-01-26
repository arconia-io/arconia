package io.arconia.core.multitenancy.context.resolvers;

import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * Strategy to use a fixed value as the current tenant, regardless of the source context.
 *
 * @author Thomas Vitale
 */
public final class FixedTenantResolver implements TenantResolver<Object> {

    private static final String DEFAULT_FIXED_TENANT = "default";

    private final String fixedTenantName;

    public FixedTenantResolver() {
        fixedTenantName = DEFAULT_FIXED_TENANT;
    }

    public FixedTenantResolver(String tenantName) {
        Assert.hasText(tenantName, "tenantName cannot be empty");
        this.fixedTenantName = tenantName;
    }

    @Override
    @NonNull
    public String resolveTenantId(Object source) {
        return fixedTenantName;
    }

}
