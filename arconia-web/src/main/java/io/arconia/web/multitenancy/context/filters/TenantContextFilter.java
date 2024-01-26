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
import io.arconia.core.multitenancy.exceptions.TenantRequiredException;
import io.arconia.web.multitenancy.context.resolvers.HttpRequestTenantResolver;

/**
 * Establish a tenant context from an HTTP request, if tenant information is available.
 *
 * @author Thomas Vitale
 */
public final class TenantContextFilter extends OncePerRequestFilter {

    private final HttpRequestTenantResolver httpRequestTenantResolver;

    private final TenantOptionalPathMatcher tenantOptionalPathMatcher;

    private final TenantEventPublisher tenantEventPublisher;

    public TenantContextFilter(HttpRequestTenantResolver httpRequestTenantResolver,
            TenantOptionalPathMatcher tenantOptionalPathMatcher, TenantEventPublisher tenantEventPublisher) {
        Assert.notNull(httpRequestTenantResolver, "httpRequestTenantResolver cannot be null");
        Assert.notNull(tenantOptionalPathMatcher, "ignorePathMatcher cannot be null");
        Assert.notNull(tenantEventPublisher, "tenantEventPublisher cannot be null");
        this.httpRequestTenantResolver = httpRequestTenantResolver;
        this.tenantOptionalPathMatcher = tenantOptionalPathMatcher;
        this.tenantEventPublisher = tenantEventPublisher;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var tenantId = httpRequestTenantResolver.resolveTenantId(request);
        if (StringUtils.hasText(tenantId)) {
            publishTenantContextAttachedEvent(tenantId, request);
        }
        else if (!tenantOptionalPathMatcher.matches(request)) {
            throw new TenantRequiredException(
                    "A tenant identifier must be specified for HTTP requests to: " + request.getRequestURI());
        }

        try {
            filterChain.doFilter(request, response);
        }
        finally {
            if (StringUtils.hasText(tenantId)) {
                publishTenantContextClosedEvent(tenantId, request);
            }
        }
    }

    private void publishTenantContextAttachedEvent(String tenantId, HttpServletRequest request) {
        var tenantContextAttachedEvent = new TenantContextAttachedEvent(tenantId, request);
        var observationContext = ServerHttpObservationFilter.findObservationContext(request);
        observationContext.ifPresent(tenantContextAttachedEvent::setObservationContext);
        tenantEventPublisher.publishTenantEvent(tenantContextAttachedEvent);
    }

    private void publishTenantContextClosedEvent(String tenantId, HttpServletRequest request) {
        var tenantContextClosedEvent = new TenantContextClosedEvent(tenantId, request);
        tenantEventPublisher.publishTenantEvent(tenantContextClosedEvent);
    }

}
