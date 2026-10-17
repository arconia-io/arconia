package io.arconia.multitenancy.web.resolution;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import io.arconia.core.support.Incubating;
import io.arconia.multitenancy.core.resolution.InvalidTenantIdentifierException;
import io.arconia.multitenancy.core.resolution.TenantResolver;

/**
 * An {@link HttpRequestTenantResolver} that consults an ordered list of resolvers and
 * returns the first tenant identifier found.
 * <p>
 * A resolver returning {@code null} or an empty value hands over to the next one. A
 * resolver throwing {@link InvalidTenantIdentifierException} stops the chain, so that
 * unusable input from one source never falls through to a less trusted one.
 */
@Incubating
public final class CompositeHttpRequestTenantResolver implements HttpRequestTenantResolver {

    private final List<TenantResolver<? super HttpServletRequest>> resolvers;

    public CompositeHttpRequestTenantResolver(List<? extends TenantResolver<? super HttpServletRequest>> resolvers) {
        Assert.notEmpty(resolvers, "resolvers cannot be null or empty");
        Assert.noNullElements(resolvers, "resolvers cannot contain null elements");
        this.resolvers = List.copyOf(resolvers);
    }

    /**
     * The resolvers consulted, in order.
     */
    public List<TenantResolver<? super HttpServletRequest>> getResolvers() {
        return resolvers;
    }

    @Override
    @Nullable
    public String resolveTenantIdentifier(HttpServletRequest request) {
        Assert.notNull(request, "request cannot be null");
        for (var resolver : resolvers) {
            var tenantIdentifier = resolver.resolveTenantIdentifier(request);
            if (StringUtils.hasText(tenantIdentifier)) {
                return tenantIdentifier;
            }
        }
        return null;
    }

}
