package io.arconia.multitenancy.core.context.resolvers;

import org.jspecify.annotations.Nullable;

import io.arconia.core.support.Incubating;

/**
 * Strategy used to resolve the current tenant from a given source context.
 */
@Incubating
@FunctionalInterface
public interface TenantResolver<T> {

    /**
     * Resolves a tenant identifier from the given source.
     */
    @Nullable
    String resolveTenantIdentifier(T source);

}
