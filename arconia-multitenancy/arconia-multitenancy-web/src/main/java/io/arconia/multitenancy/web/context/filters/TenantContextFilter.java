package io.arconia.multitenancy.web.context.filters;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.filter.ServerHttpObservationFilter;

import io.arconia.core.support.Incubating;
import io.arconia.multitenancy.core.context.events.TenantContextAttachedEvent;
import io.arconia.multitenancy.core.context.events.TenantContextClosedEvent;
import io.arconia.multitenancy.core.events.TenantEventPublisher;
import io.arconia.multitenancy.core.exceptions.TenantResolutionException;
import io.arconia.multitenancy.web.context.resolvers.HttpRequestTenantResolver;

/**
 * Establish a tenant context from an HTTP request, if tenant information is available.
 */
@Incubating(since = "0.1.0")
public final class TenantContextFilter extends OncePerRequestFilter {

    private static final String MISSING_TENANT_ERROR_MESSAGE = "A tenant identifier must be specified for HTTP requests to %s";

    private final HttpRequestTenantResolver httpRequestTenantResolver;

    private final TenantContextIgnorePathMatcher tenantContextIgnorePathMatcher;

    private final TenantEventPublisher tenantEventPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
            handleTenantResolutionException(response, MISSING_TENANT_ERROR_MESSAGE.formatted(request.getRequestURI()));
            return;
        }

        try {
            publishTenantContextAttachedEvent(tenantIdentifier, request);
        }
        catch (TenantResolutionException exception) {
            publishTenantContextClosedEvent(tenantIdentifier, request);
            handleTenantResolutionException(response, exception.getMessage());
            return;
        }

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

    private void handleTenantResolutionException(HttpServletResponse response, String exceptionMessage)
            throws IOException {
        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exceptionMessage);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
    }

}
