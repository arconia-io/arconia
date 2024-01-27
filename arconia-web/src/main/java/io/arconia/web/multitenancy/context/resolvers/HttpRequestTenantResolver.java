package io.arconia.web.multitenancy.context.resolvers;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;

import io.arconia.core.multitenancy.context.resolvers.TenantResolver;

/**
 * Strategy used to resolve the current tenant from an HTTP request.
 *
 * @author Thomas Vitale
 */
public interface HttpRequestTenantResolver extends TenantResolver<HttpServletRequest> {

    /**
     * Resolves a tenant identifier from an HTTP request.
     */
    @Nullable
    String resolveTenantIdentifier(HttpServletRequest request);

}
