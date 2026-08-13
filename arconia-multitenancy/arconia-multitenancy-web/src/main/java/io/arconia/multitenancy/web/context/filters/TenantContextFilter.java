package io.arconia.multitenancy.web.context.filters;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.micrometer.common.KeyValue;

import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.filter.ServerHttpObservationFilter;
import org.springframework.web.util.WebUtils;

import tools.jackson.databind.json.JsonMapper;

import io.arconia.core.support.Incubating;
import io.arconia.multitenancy.core.context.TenantContext;
import io.arconia.multitenancy.core.context.events.TenantContextAttachedEvent;
import io.arconia.multitenancy.core.context.events.TenantContextClosedEvent;
import io.arconia.multitenancy.core.exceptions.TenantVerificationException;
import io.arconia.multitenancy.core.observability.Cardinality;
import io.arconia.multitenancy.core.observability.TenantObservationFilter;
import io.arconia.multitenancy.core.tenantdetails.DefaultTenantIdentifierValidator;
import io.arconia.multitenancy.core.tenantdetails.TenantIdentifierValidator;
import io.arconia.multitenancy.core.tenantdetails.TenantVerifier;
import io.arconia.multitenancy.web.context.resolvers.HttpRequestTenantResolver;

/**
 * Establish a tenant context from an HTTP request, if tenant information is available.
 */
@Incubating
public final class TenantContextFilter extends OncePerRequestFilter implements Ordered {

    private static final String MISSING_TENANT_ERROR_MESSAGE = "A tenant identifier must be specified for HTTP requests to %s";

    private static final String TENANT_LOOKUP_ERROR_MESSAGE = "The tenant could not be verified because the tenant registry is currently unavailable";

    private final int order;

    private final HttpRequestTenantResolver httpRequestTenantResolver;

    private final TenantContextIgnorePathMatcher tenantContextIgnorePathMatcher;

    private final ApplicationEventPublisher eventPublisher;

    private final TenantIdentifierValidator tenantIdentifierValidator;

    @Nullable
    private final TenantVerifier tenantVerifier;

    @Nullable
    private final TenantObservationFilter tenantObservationFilter;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private TenantContextFilter(int order, HttpRequestTenantResolver httpRequestTenantResolver,
            TenantContextIgnorePathMatcher tenantContextIgnorePathMatcher, ApplicationEventPublisher eventPublisher,
            TenantIdentifierValidator tenantIdentifierValidator, @Nullable TenantVerifier tenantVerifier,
            @Nullable TenantObservationFilter tenantObservationFilter) {
        Assert.notNull(httpRequestTenantResolver, "httpRequestTenantResolver cannot be null");
        Assert.notNull(tenantContextIgnorePathMatcher, "tenantContextIgnorePathMatcher cannot be null");
        Assert.notNull(eventPublisher, "eventPublisher cannot be null");
        Assert.notNull(tenantIdentifierValidator, "tenantIdentifierValidator cannot be null");
        this.order = order;
        this.httpRequestTenantResolver = httpRequestTenantResolver;
        this.tenantContextIgnorePathMatcher = tenantContextIgnorePathMatcher;
        this.eventPublisher = eventPublisher;
        this.tenantIdentifierValidator = tenantIdentifierValidator;
        this.tenantVerifier = tenantVerifier;
        this.tenantObservationFilter = tenantObservationFilter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // On an ERROR dispatch the response is already being written by the container's
        // error handling. A tenant that cannot be resolved must not produce a second
        // response here, so the request proceeds unbound instead.
        var errorDispatch = request.getAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE) != null;

        var tenantIdentifier = httpRequestTenantResolver.resolveTenantIdentifier(request);
        if (!StringUtils.hasText(tenantIdentifier)) {
            if (errorDispatch) {
                filterChain.doFilter(request, response);
                return;
            }
            handleProblem(response, HttpStatus.BAD_REQUEST,
                    MISSING_TENANT_ERROR_MESSAGE.formatted(request.getRequestURI()));
            return;
        }

        try {
            tenantIdentifierValidator.validate(tenantIdentifier);
            if (tenantVerifier != null) {
                tenantVerifier.verify(tenantIdentifier);
            }
        }
        catch (TenantVerificationException exception) {
            if (errorDispatch) {
                filterChain.doFilter(request, response);
                return;
            }
            handleProblem(response, HttpStatus.BAD_REQUEST, exception.getMessage());
            return;
        }
        catch (RuntimeException exception) {
            // The tenant registry itself failed, for example, because the datastore
            // backing the TenantDetailsService is unreachable. That is not the caller's
            // fault, so it degrades to a controlled 503 rather than an unhandled 500.
            logger.error("Tenant verification failed unexpectedly", exception);
            if (errorDispatch) {
                filterChain.doFilter(request, response);
                return;
            }
            handleProblem(response, HttpStatus.SERVICE_UNAVAILABLE, TENANT_LOOKUP_ERROR_MESSAGE);
            return;
        }

        // The HTTP server observation is created by ServerHttpObservationFilter before a
        // tenant is bound, so the core TenantObservationFilter never sees it. Without
        // this, only the child observations would carry the tenant identifier and the
        // outer server span would not.
        if (tenantObservationFilter != null) {
            ServerHttpObservationFilter.findObservationContext(request).ifPresent(ctx -> {
                var keyValue = KeyValue.of(tenantObservationFilter.getTenantIdentifierKey(), tenantIdentifier);
                if (tenantObservationFilter.getCardinality() == Cardinality.LOW) {
                    ctx.addLowCardinalityKeyValue(keyValue);
                } else {
                    ctx.addHighCardinalityKeyValue(keyValue);
                }
            });
        }

        try {
            TenantContext.where(tenantIdentifier).call(() -> {

                try {
                    eventPublisher.publishEvent(new TenantContextAttachedEvent(tenantIdentifier, request));
                    filterChain.doFilter(request, response);
                }
                finally {
                    eventPublisher.publishEvent(new TenantContextClosedEvent(tenantIdentifier, request));
                }
                return null;
            });
        }
        catch (Exception ex) {
            // ScopedValue.Carrier.call infers a single thrown type, which widens to
            // Exception here, while this method may only declare the two checked types
            // below. Unchecked exceptions are rethrown as they are, so that what reaches
            // @ControllerAdvice and error handling is unchanged.
            switch (ex) {
                case ServletException se -> throw se;
                case IOException ioe -> throw ioe;
                case RuntimeException re -> throw re;
                default -> throw new ServletException(ex);
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return tenantContextIgnorePathMatcher.matches(request);
    }

    /**
     * The tenant context is re-established on an ERROR dispatch, so that error handling
     * runs with the same tenant as the request that failed.
     */
    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    @Override
    public int getOrder() {
        return order;
    }

    private void handleProblem(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        var problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setStatus(status.value());
        response.getWriter().write(jsonMapper.writeValueAsString(problemDetail));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private int order = Ordered.LOWEST_PRECEDENCE;

        private HttpRequestTenantResolver httpRequestTenantResolver;

        private TenantContextIgnorePathMatcher tenantContextIgnorePathMatcher;

        private ApplicationEventPublisher eventPublisher;

        private TenantIdentifierValidator tenantIdentifierValidator = DefaultTenantIdentifierValidator.builder()
            .build();

        @Nullable
        private TenantVerifier tenantVerifier;

        @Nullable
        private TenantObservationFilter tenantObservationFilter;

        private Builder() {}

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public Builder httpRequestTenantResolver(HttpRequestTenantResolver httpRequestTenantResolver) {
            this.httpRequestTenantResolver = httpRequestTenantResolver;
            return this;
        }

        public Builder tenantContextIgnorePathMatcher(TenantContextIgnorePathMatcher tenantContextIgnorePathMatcher) {
            this.tenantContextIgnorePathMatcher = tenantContextIgnorePathMatcher;
            return this;
        }

        public Builder eventPublisher(ApplicationEventPublisher eventPublisher) {
            this.eventPublisher = eventPublisher;
            return this;
        }

        /**
         * Validator applied to every resolved tenant identifier.
         */
        public Builder tenantIdentifierValidator(TenantIdentifierValidator tenantIdentifierValidator) {
            this.tenantIdentifierValidator = tenantIdentifierValidator;
            return this;
        }

        /**
         * Verifier checking that the resolved tenant exists and is enabled. Optional.
         */
        public Builder tenantVerifier(@Nullable TenantVerifier tenantVerifier) {
            this.tenantVerifier = tenantVerifier;
            return this;
        }

        public Builder tenantObservationFilter(@Nullable TenantObservationFilter tenantObservationFilter) {
            this.tenantObservationFilter = tenantObservationFilter;
            return this;
        }

        public TenantContextFilter build() {
            return new TenantContextFilter(order, httpRequestTenantResolver, tenantContextIgnorePathMatcher,
                    eventPublisher, tenantIdentifierValidator, tenantVerifier, tenantObservationFilter);
        }

    }

}
