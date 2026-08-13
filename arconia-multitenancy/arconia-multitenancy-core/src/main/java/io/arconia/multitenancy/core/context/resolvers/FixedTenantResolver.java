package io.arconia.multitenancy.core.context.resolvers;

import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;

/**
 * Strategy to use a fixed value as the current tenant, regardless of the source context.
 */
@Incubating
public final class FixedTenantResolver implements TenantResolver<Object> {

    private static final String DEFAULT_FIXED_TENANT_IDENTIFIER = "default";

    private final String fixedTenantIdentifier;

    private FixedTenantResolver(String tenantIdentifier) {
        Assert.hasText(tenantIdentifier, "tenantIdentifier cannot be null or empty");
        this.fixedTenantIdentifier = tenantIdentifier;
    }

    @Override
    public String resolveTenantIdentifier(Object source) {
        return fixedTenantIdentifier;
    }

    /**
     * Creates a new builder for {@link FixedTenantResolver}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link FixedTenantResolver}.
     */
    public static final class Builder {

        private String tenantIdentifier = DEFAULT_FIXED_TENANT_IDENTIFIER;

        private Builder() {}

        /**
         * Identifier of the fixed tenant to use in each context.
         */
        public Builder tenantIdentifier(String tenantIdentifier) {
            this.tenantIdentifier = tenantIdentifier;
            return this;
        }

        /**
         * Builds the {@link FixedTenantResolver} instance.
         */
        public FixedTenantResolver build() {
            return new FixedTenantResolver(tenantIdentifier);
        }

    }

}
