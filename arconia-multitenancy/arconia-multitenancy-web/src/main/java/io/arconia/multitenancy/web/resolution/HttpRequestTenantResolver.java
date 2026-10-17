package io.arconia.multitenancy.web.resolution;

import jakarta.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;

import io.arconia.core.support.Incubating;
import io.arconia.multitenancy.core.resolution.InvalidTenantIdentifierException;
import io.arconia.multitenancy.core.resolution.TenantResolver;

/**
 * Strategy used to resolve the current tenant from an HTTP request.
 */
@Incubating
public interface HttpRequestTenantResolver extends TenantResolver<HttpServletRequest> {

    /**
     * Resolves a tenant identifier from an HTTP request, or returns {@code null} when the
     * request carries nothing this resolver handles.
     * @throws InvalidTenantIdentifierException if the request carries tenant information
     * that is present but unusable
     */
    @Override
    @Nullable
    String resolveTenantIdentifier(HttpServletRequest request);

}
