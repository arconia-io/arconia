package io.arconia.multitenancy.core.resolution;

import org.jspecify.annotations.Nullable;

import io.arconia.core.support.Incubating;

/**
 * Strategy used to resolve the current tenant from a given source context.
 * <p>
 * A resolver has three possible outcomes:
 * <ul>
 * <li>it returns a tenant identifier when the source carries one;</li>
 * <li>it returns {@code null} when the source carries nothing this resolver handles, so
 * that another resolver may be consulted;</li>
 * <li>it throws {@link InvalidTenantIdentifierException} when the source carries tenant
 * information that is present but unusable, such as conflicting values or an
 * authenticated principal without a tenant claim. No further resolver is consulted in
 * that case, so that unusable input from one source never falls through to a less
 * trusted one.</li>
 * </ul>
 * Exception messages may be logged and returned to the caller, so they should describe
 * the rule that was violated rather than embed the raw source value.
 */
@Incubating
@FunctionalInterface
public interface TenantResolver<T> {

    /**
     * Resolves a tenant identifier from the given source, or returns {@code null} when
     * the source carries nothing this resolver handles.
     * @throws InvalidTenantIdentifierException if the source carries tenant information
     * that is present but unusable
     */
    @Nullable
    String resolveTenantIdentifier(T source);

}
