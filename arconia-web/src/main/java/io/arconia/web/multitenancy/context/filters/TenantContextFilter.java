package io.arconia.web.multitenancy.context.filters;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.filter.ServerHttpObservationFilter;

import io.arconia.core.multitenancy.context.events.TenantContextAttachedEvent;
import io.arconia.core.multitenancy.context.events.TenantContextClosedEvent;
import io.arconia.core.multitenancy.events.TenantEventPublisher;
import io.arconia.core.multitenancy.exceptions.TenantResolutionException;
import io.arconia.web.multitenancy.context.resolvers.HttpRequestTenantResolver;

/**
 * Establish a tenant context from an HTTP request, if tenant information is available.
 *
 * @author Thomas Vitale
 */
public final class TenantContextFilter extends OncePerRequestFilter {

    private final HttpRequestTenantResolver httpRequestTenantResolver;

    private final TenantContextIgnorePathMatcher tenantContextIgnorePathMatcher;

    private final TenantEventPublisher tenantEventPublisher;

    public TenantContextFilter(HttpRequestTenantResolver httpRequestTenantResolver,
            TenantContextIgnorePathMatcher tenantContextIgnorePathMatcher, TenantEventPublisher tenantEventPublisher) {
        Assert.notNull(httpRequestTenantResolver, "httpRequestTenantResolver cannot be null");
        Assert.notNull(tenantContextIgnorePathMatcher, "ignorePathMatcher cannot be null");
        Assert.notNull(tenantEventPublisher, "tenantEventPublisher cannot be null");
        this.httpRequestTenantResolver = httpRequestTenantResolver;
        this.tenantContextIgnorePathMatcher = tenantContextIgnorePathMatcher;
        this.tenantEventPublisher = tenantEventPublisher;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var tenantIdentifier = httpRequestTenantResolver.resolveTenantIdentifier(request);
        if (!StringUtils.hasText(tenantIdentifier)) {
            throw new TenantResolutionException(
                    "A tenant identifier must be specified for HTTP requests to: " + request.getRequestURI());
        }
        publishTenantContextAttachedEvent(tenantIdentifier, request);

        try {
            filterChain.doFilter(request, response);
        }
        finally {
            publishTenantContextClosedEvent(tenantIdentifier, request);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return tenantContextIgnorePathMatcher.matches(request);
    }

    private void publishTenantContextAttachedEvent(String tenantIdentifier, HttpServletRequest request) {
        var tenantContextAttachedEvent = new TenantContextAttachedEvent(tenantIdentifier, request);
        var observationContext = ServerHttpObservationFilter.findObservationContext(request);
        observationContext.ifPresent(tenantContextAttachedEvent::setObservationContext);
        tenantEventPublisher.publishTenantEvent(tenantContextAttachedEvent);
    }

    private void publishTenantContextClosedEvent(String tenantIdentifier, HttpServletRequest request) {
        var tenantContextClosedEvent = new TenantContextClosedEvent(tenantIdentifier, request);
        tenantEventPublisher.publishTenantEvent(tenantContextClosedEvent);
    }

}
