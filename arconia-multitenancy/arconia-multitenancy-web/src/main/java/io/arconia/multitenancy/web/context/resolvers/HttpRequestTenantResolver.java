package io.arconia.multitenancy.web.context.resolvers;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;

import io.arconia.multitenancy.core.context.resolvers.TenantResolver;

/**
 * Strategy used to resolve the current tenant from an HTTP request.
 */
public interface HttpRequestTenantResolver extends TenantResolver<HttpServletRequest> {

    /**
     * Resolves a tenant identifier from an HTTP request.
     */
    @Nullable
    String resolveTenantIdentifier(HttpServletRequest request);

}
