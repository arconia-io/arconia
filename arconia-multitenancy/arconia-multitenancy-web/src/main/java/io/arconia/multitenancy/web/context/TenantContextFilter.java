package io.arconia.multitenancy.web.context;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.filter.ServerHttpObservationFilter;
import org.springframework.web.util.WebUtils;

import io.arconia.core.support.Incubating;
import io.arconia.multitenancy.core.context.TenantContext;
import io.arconia.multitenancy.core.context.TenantContextAttachedEvent;
import io.arconia.multitenancy.core.context.TenantContextClosedEvent;
import io.arconia.multitenancy.core.observability.TenantObservationFilter;
import io.arconia.multitenancy.core.resolution.DefaultTenantIdentifierValidator;
import io.arconia.multitenancy.core.resolution.InvalidTenantIdentifierException;
import io.arconia.multitenancy.core.resolution.TenantIdentifierValidator;
import io.arconia.multitenancy.core.resolution.TenantIdentifiers;
import io.arconia.multitenancy.core.resolution.TenantResolver;
import io.arconia.multitenancy.core.resolution.TenantVerificationException;
import io.arconia.multitenancy.core.resolution.TenantVerifier;
import io.arconia.multitenancy.web.resolution.ProblemDetailTenantResolutionFailureHandler;
import io.arconia.multitenancy.web.resolution.TenantResolutionFailure;
import io.arconia.multitenancy.web.resolution.TenantResolutionFailure.Outcome;
import io.arconia.multitenancy.web.resolution.TenantResolutionFailureHandler;

/**
 * Establishes a tenant context for the duration of an HTTP request.
 * <p>
 * The tenant identifier is resolved by a {@link TenantResolver}, validated by a
 * {@link TenantIdentifierValidator}, optionally verified by a {@link TenantVerifier}, and
 * then bound to the {@link TenantContext} while the rest of the filter chain runs. When
 * no tenant context can be established, the request is handed to a
 * {@link TenantResolutionFailureHandler}.
 */
@Incubating
public final class TenantContextFilter extends OncePerRequestFilter implements Ordered {

    private static final String MISSING_TENANT_MESSAGE = "A tenant identifier must be specified for the HTTP request";

    private static final String TENANT_LOOKUP_ERROR_MESSAGE = "The tenant could not be verified because the tenant registry is currently unavailable";

    private final int order;

    private final TenantResolver<HttpServletRequest> tenantResolver;

    private final TenantContextIgnorePathMatcher tenantContextIgnorePathMatcher;

    private final ApplicationEventPublisher eventPublisher;

    private final TenantIdentifierValidator tenantIdentifierValidator;

    @Nullable
    private final TenantVerifier tenantVerifier;

    @Nullable
    private final TenantObservationFilter tenantObservationFilter;

    private final TenantResolutionFailureHandler failureHandler;

    private TenantContextFilter(int order, TenantResolver<HttpServletRequest> tenantResolver,
            TenantContextIgnorePathMatcher tenantContextIgnorePathMatcher, ApplicationEventPublisher eventPublisher,
            TenantIdentifierValidator tenantIdentifierValidator, @Nullable TenantVerifier tenantVerifier,
            @Nullable TenantObservationFilter tenantObservationFilter, TenantResolutionFailureHandler failureHandler) {
        Assert.notNull(tenantResolver, "httpRequestTenantResolver cannot be null");
        Assert.notNull(tenantContextIgnorePathMatcher, "tenantContextIgnorePathMatcher cannot be null");
        Assert.notNull(eventPublisher, "eventPublisher cannot be null");
        Assert.notNull(tenantIdentifierValidator, "tenantIdentifierValidator cannot be null");
        Assert.notNull(failureHandler, "failureHandler cannot be null");
        this.order = order;
        this.tenantResolver = tenantResolver;
        this.tenantContextIgnorePathMatcher = tenantContextIgnorePathMatcher;
        this.eventPublisher = eventPublisher;
        this.tenantIdentifierValidator = tenantIdentifierValidator;
        this.tenantVerifier = tenantVerifier;
        this.tenantObservationFilter = tenantObservationFilter;
        this.failureHandler = failureHandler;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String tenantIdentifier;
        try {
            tenantIdentifier = tenantResolver.resolveTenantIdentifier(request);
        } catch (InvalidTenantIdentifierException ex) {
            handleFailure(request, response, filterChain, Outcome.INVALID, ex.getMessage(), ex, null);
            return;
        }
        if (!StringUtils.hasText(tenantIdentifier)) {
            handleFailure(request, response, filterChain, Outcome.MISSING, MISSING_TENANT_MESSAGE, null, null);
            return;
        }

        try {
            tenantIdentifierValidator.validate(tenantIdentifier);
            if (tenantVerifier != null) {
                tenantVerifier.verify(tenantIdentifier);
            }
        } catch (InvalidTenantIdentifierException ex) {
            handleFailure(request, response, filterChain, Outcome.INVALID, ex.getMessage(), ex, tenantIdentifier);
            return;
        } catch (TenantVerificationException ex) {
            handleFailure(request, response, filterChain, Outcome.REJECTED, ex.getMessage(), ex, tenantIdentifier);
            return;
        } catch (RuntimeException ex) {
            // The tenant registry itself failed, for example, because the datastore
            // backing the TenantDetailsService is unreachable. That is not the caller's
            // fault, so it degrades to a controlled 503 rather than an unhandled 500.
            logger.error("Tenant verification failed unexpectedly", ex);
            handleFailure(request, response, filterChain, Outcome.UNAVAILABLE, TENANT_LOOKUP_ERROR_MESSAGE, ex, tenantIdentifier);
            return;
        }

        // The HTTP server observation is created by ServerHttpObservationFilter before a
        // tenant is bound, so the core TenantObservationFilter never sees it. Without
        // this, only the child observations would carry the tenant identifier and the
        // outer server span would not.
        if (tenantObservationFilter != null) {
            ServerHttpObservationFilter.findObservationContext(request)
                .ifPresent(context -> tenantObservationFilter.enrich(context, tenantIdentifier));
        }

        try {
            TenantContext.where(tenantIdentifier).call(() -> {
                try {
                    eventPublisher.publishEvent(new TenantContextAttachedEvent(tenantIdentifier, request));
                    filterChain.doFilter(request, response);
                } finally {
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

    /**
     * On an ERROR dispatch the response is already being written by the container's
     * error handling. A tenant that cannot be resolved must not produce a second response
     * here, so the request proceeds unbound instead. In every other case, the failure is
     * logged with the identifier in a log-safe form since it is unvalidated client input
     * and handed to the failure handler.
     */
    private void handleFailure(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain,
            Outcome outcome, String message, @Nullable Throwable cause, @Nullable String tenantIdentifier)
            throws ServletException, IOException {
        if (logger.isDebugEnabled()) {
            var identifier = tenantIdentifier != null ? " '" + TenantIdentifiers.sanitizeForLogging(tenantIdentifier) + "'" : "";
            logger.debug("No tenant context established for request to %s (%s%s): %s".formatted(request.getRequestURI(),
                    outcome.name().toLowerCase(java.util.Locale.ROOT), identifier, message));
        }
        if (request.getAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE) != null) {
            filterChain.doFilter(request, response);
            return;
        }
        failureHandler.handle(request, response, new TenantResolutionFailure(outcome, message, cause));
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

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private int order = Ordered.LOWEST_PRECEDENCE;

        private TenantResolver<HttpServletRequest> tenantResolver;

        private TenantContextIgnorePathMatcher tenantContextIgnorePathMatcher;

        private ApplicationEventPublisher eventPublisher;

        private TenantIdentifierValidator tenantIdentifierValidator = DefaultTenantIdentifierValidator.builder()
            .build();

        @Nullable
        private TenantVerifier tenantVerifier;

        @Nullable
        private TenantObservationFilter tenantObservationFilter;

        private TenantResolutionFailureHandler failureHandler = new ProblemDetailTenantResolutionFailureHandler();

        private Builder() {}

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        /**
         * Resolver of the tenant identifier from the HTTP request.
         */
        public Builder httpRequestTenantResolver(TenantResolver<HttpServletRequest> tenantResolver) {
            this.tenantResolver = tenantResolver;
            return this;
        }

        /**
         * Matcher that determines whether a tenant context should be established for
         * the given request.
         */
        public Builder tenantContextIgnorePathMatcher(TenantContextIgnorePathMatcher tenantContextIgnorePathMatcher) {
            this.tenantContextIgnorePathMatcher = tenantContextIgnorePathMatcher;
            return this;
        }

        /**
         * Application event publisher used to publish tenant context events.
         */
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

        /**
         * Observation filter that enriches the server span and metrics with the tenant identifier. Optional.
         */
        public Builder tenantObservationFilter(@Nullable TenantObservationFilter tenantObservationFilter) {
            this.tenantObservationFilter = tenantObservationFilter;
            return this;
        }

        /**
         * Handler completing the response when no tenant context can be established.
         */
        public Builder failureHandler(TenantResolutionFailureHandler failureHandler) {
            this.failureHandler = failureHandler;
            return this;
        }

        public TenantContextFilter build() {
            return new TenantContextFilter(order, tenantResolver, tenantContextIgnorePathMatcher, eventPublisher,
                    tenantIdentifierValidator, tenantVerifier, tenantObservationFilter, failureHandler);
        }

    }

}
