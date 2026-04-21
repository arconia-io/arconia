package io.arconia.multitenancy.web.context.filters;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import tools.jackson.databind.json.JsonMapper;

import io.arconia.core.support.Incubating;
import io.arconia.multitenancy.core.context.TenantContext;
import io.arconia.multitenancy.core.context.events.TenantContextAttachedEvent;
import io.arconia.multitenancy.core.context.events.TenantContextClosedEvent;
import io.arconia.multitenancy.core.exceptions.TenantVerificationException;
import io.arconia.multitenancy.core.tenantdetails.TenantVerifier;
import io.arconia.multitenancy.web.context.resolvers.HttpRequestTenantResolver;

/**
 * Establish a tenant context from an HTTP request, if tenant information is available.
 */
@Incubating
public final class TenantContextFilter extends OncePerRequestFilter {

    private static final String MISSING_TENANT_ERROR_MESSAGE = "A tenant identifier must be specified for HTTP requests to %s";

    private final HttpRequestTenantResolver httpRequestTenantResolver;

    private final TenantContextIgnorePathMatcher tenantContextIgnorePathMatcher;

    private final ApplicationEventPublisher eventPublisher;

    @Nullable
    private final TenantVerifier tenantVerifier;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public TenantContextFilter(HttpRequestTenantResolver httpRequestTenantResolver,
            TenantContextIgnorePathMatcher tenantContextIgnorePathMatcher, ApplicationEventPublisher eventPublisher,
            @Nullable TenantVerifier tenantVerifier) {
        Assert.notNull(httpRequestTenantResolver, "httpRequestTenantResolver cannot be null");
        Assert.notNull(tenantContextIgnorePathMatcher, "ignorePathMatcher cannot be null");
        Assert.notNull(eventPublisher, "eventPublisher cannot be null");
        this.httpRequestTenantResolver = httpRequestTenantResolver;
        this.tenantContextIgnorePathMatcher = tenantContextIgnorePathMatcher;
        this.eventPublisher = eventPublisher;
        this.tenantVerifier = tenantVerifier;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var tenantIdentifier = httpRequestTenantResolver.resolveTenantIdentifier(request);
        if (!StringUtils.hasText(tenantIdentifier)) {
            handleTenantVerificationException(response, MISSING_TENANT_ERROR_MESSAGE.formatted(request.getRequestURI()));
            return;
        }

        if (tenantVerifier != null) {
            try {
                tenantVerifier.verify(tenantIdentifier);
            }
            catch (TenantVerificationException exception) {
                handleTenantVerificationException(response, exception.getMessage());
                return;
            }
        }

        try {
            TenantContext.where(tenantIdentifier).call(() -> {
                eventPublisher.publishEvent(new TenantContextAttachedEvent(tenantIdentifier, request));
                try {
                    filterChain.doFilter(request, response);
                }
                finally {
                    eventPublisher.publishEvent(new TenantContextClosedEvent(tenantIdentifier, request));
                }
                return null;
            });
        }
        catch (Exception ex) {
            switch (ex) {
                case ServletException se -> throw se;
                case IOException ioe -> throw ioe;
                default -> throw new ServletException(ex);
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return tenantContextIgnorePathMatcher.matches(request);
    }

    private void handleTenantVerificationException(HttpServletResponse response, String exceptionMessage)
            throws IOException {
        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exceptionMessage);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.getWriter().write(jsonMapper.writeValueAsString(problemDetail));
    }

}
