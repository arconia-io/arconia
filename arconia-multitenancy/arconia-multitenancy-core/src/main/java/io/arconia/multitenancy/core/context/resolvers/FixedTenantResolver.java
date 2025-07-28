package io.arconia.multitenancy.core.context.resolvers;

import org.jspecify.annotations.NonNull;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;

/**
 * Strategy to use a fixed value as the current tenant, regardless of the source context.
 */
@Incubating(since = "0.1.0")
public final class FixedTenantResolver implements TenantResolver<Object> {

    public static final String DEFAULT_FIXED_TENANT_IDENTIFIER = "default";

    private final String fixedTenantIdentifier;

    public FixedTenantResolver() {
        fixedTenantIdentifier = DEFAULT_FIXED_TENANT_IDENTIFIER;
    }

    public FixedTenantResolver(String tenantIdentifier) {
        Assert.hasText(tenantIdentifier, "tenantIdentifier cannot be null or empty");
        this.fixedTenantIdentifier = tenantIdentifier;
    }

    @Override
    @NonNull
    public String resolveTenantIdentifier(Object source) {
        return fixedTenantIdentifier;
    }

}
