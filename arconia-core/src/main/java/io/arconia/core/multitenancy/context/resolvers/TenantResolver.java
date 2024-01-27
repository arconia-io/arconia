package io.arconia.core.multitenancy.context.resolvers;

import org.springframework.lang.Nullable;

/**
 * Strategy used to resolve the current tenant from a given source context.
 *
 * @author Thomas Vitale
 */
@FunctionalInterface
public interface TenantResolver<T> {

    /**
     * Resolves a tenant identifier from the given source.
     */
    @Nullable
    String resolveTenantIdentifier(T source);

}
