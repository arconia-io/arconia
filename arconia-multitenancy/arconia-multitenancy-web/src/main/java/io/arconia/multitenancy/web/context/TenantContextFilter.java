package io.arconia.multitenancy.web.context;

import java.io.IOException;
import java.util.Set;

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
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.filter.ServerHttpObservationFilter;
import org.springframework.web.util.WebUtils;

import io.arconia.core.annotations.Incubating;
import io.arconia.multitenancy.core.context.TenantContext;
import io.arconia.multitenancy.core.context.TenantContextAttachedEvent;
import io.arconia.multitenancy.core.context.TenantContextClosedEvent;
import io.arconia.multitenancy.core.observability.TenantObservationFilter;
import io.arconia.multitenancy.core.resolution.DefaultTenantIdentifierValidator;
import io.arconia.multitenancy.core.resolution.InvalidTenantIdentifierException;
import io.arconia.multitenancy.core.resolution.TenantIdentifierValidator;
import io.arconia.multitenancy.core.resolution.TenantResolver;
import io.arconia.multitenancy.core.resolution.TenantVerificationException;
import io.arconia.multitenancy.core.resolution.TenantVerifier;
import io.arconia.multitenancy.web.resolution.ProblemDetailTenantResolutionFailureHandler;
import io.arconia.multitenancy.web.resolution.TenantResolutionFailure.Outcome;
import io.arconia.multitenancy.web.resolution.TenantResolutionFailure;
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

    private static final String INVALID_TENANT_MESSAGE = "The tenant identifier is not valid";

    private static final String REJECTED_TENANT_MESSAGE = "The tenant was rejected";

    private static final int LOG_MAX_LENGTH = 64;

    private static final char LOG_REPLACEMENT_CHAR = '\uFFFD';

    /**
     * Request attribute holding the tenant identifier established on the initial
     * dispatch. Clients cannot set request attributes, so its value is trusted.
     */
    private static final String TENANT_IDENTIFIER_ATTRIBUTE = TenantContextFilter.class.getName() + ".TENANT_IDENTIFIER";

    private final int order;

    private final TenantResolver<HttpServletRequest> tenantResolver;

    private final TenantContextIgnorePathMatcher ignorePathMatcher;

    private final ApplicationEventPublisher eventPublisher;

    private final TenantIdentifierValidator tenantIdentifierValidator;

    @Nullable
    private final TenantVerifier tenantVerifier;

    @Nullable
    private final TenantObservationFilter tenantObservationFilter;

    private final TenantResolutionFailureHandler failureHandler;

    private TenantContextFilter(int order, TenantResolver<HttpServletRequest> tenantResolver,
            Set<String> ignorePaths, ApplicationEventPublisher eventPublisher,
            TenantIdentifierValidator tenantIdentifierValidator, @Nullable TenantVerifier tenantVerifier,
            @Nullable TenantObservationFilter tenantObservationFilter, TenantResolutionFailureHandler failureHandler) {
        Assert.notNull(tenantResolver, "tenantResolver cannot be null");
        Assert.notNull(ignorePaths, "ignorePaths cannot be null");
        Assert.notNull(eventPublisher, "eventPublisher cannot be null");
        Assert.notNull(tenantIdentifierValidator, "tenantIdentifierValidator cannot be null");
        Assert.notNull(failureHandler, "failureHandler cannot be null");
        this.order = order;
        this.tenantResolver = tenantResolver;
        this.ignorePathMatcher = new TenantContextIgnorePathMatcher(ignorePaths);
        this.eventPublisher = eventPublisher;
        this.tenantIdentifierValidator = tenantIdentifierValidator;
        this.tenantVerifier = tenantVerifier;
        this.tenantObservationFilter = tenantObservationFilter;
        this.failureHandler = failureHandler;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // The tenant established on the initial dispatch is bound again, as it is, on the
        // ASYNC and ERROR dispatches of the same request. Resolving it again could give
        // another result, since an ERROR dispatch has the error page as its request URI.
        var tenantIdentifier = request.getAttribute(TENANT_IDENTIFIER_ATTRIBUTE) instanceof String established
                ? established : establishTenant(request, response, filterChain);
        if (tenantIdentifier == null) {
            return;
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
     * Resolves, validates, and verifies the tenant identifier, and records it on the
     * request for the later dispatches. Returns {@code null} once the failure has been
     * handled when no tenant context can be established.
     */
    @Nullable
    private String establishTenant(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String tenantIdentifier;
        try {
            tenantIdentifier = tenantResolver.resolveTenantIdentifier(request);
        } catch (InvalidTenantIdentifierException ex) {
            handleFailure(request, response, filterChain, Outcome.INVALID, messageOf(ex, INVALID_TENANT_MESSAGE), ex, null);
            return null;
        }
        // Only null means absent. A blank value was found, so the validator rejects it
        // rather than treating the tenant as missing.
        if (tenantIdentifier == null) {
            handleFailure(request, response, filterChain, Outcome.MISSING, MISSING_TENANT_MESSAGE, null, null);
            return null;
        }

        try {
            tenantIdentifierValidator.validate(tenantIdentifier);
        } catch (InvalidTenantIdentifierException ex) {
            handleFailure(request, response, filterChain, Outcome.INVALID, messageOf(ex, INVALID_TENANT_MESSAGE), ex, tenantIdentifier);
            return null;
        }

        if (tenantVerifier != null) {
            try {
                tenantVerifier.verify(tenantIdentifier);
            } catch (InvalidTenantIdentifierException ex) {
                handleFailure(request, response, filterChain, Outcome.INVALID, messageOf(ex, INVALID_TENANT_MESSAGE), ex, tenantIdentifier);
                return null;
            } catch (TenantVerificationException ex) {
                handleFailure(request, response, filterChain, Outcome.REJECTED, messageOf(ex, REJECTED_TENANT_MESSAGE), ex, tenantIdentifier);
                return null;
            } catch (RuntimeException ex) {
                // The tenant registry itself failed, for example, because the datastore
                // backing the TenantDetailsService is unreachable. That is not the caller's
                // fault, so it degrades to a controlled 503 rather than an unhandled 500.
                // An outage repeats this on every request, so the stack trace is only
                // logged at debug level.
                if (logger.isDebugEnabled()) {
                    logger.warn("Tenant verification failed unexpectedly", ex);
                } else {
                    logger.warn("Tenant verification failed unexpectedly: " + ex);
                }
                handleFailure(request, response, filterChain, Outcome.UNAVAILABLE, TENANT_LOOKUP_ERROR_MESSAGE, ex, tenantIdentifier);
                return null;
            }
        }

        // The HTTP server observation is created by ServerHttpObservationFilter before a
        // tenant is bound, so the core TenantObservationFilter never sees it. Without
        // this, only the child observations would carry the tenant identifier and the
        // outer server span would not.
        if (tenantObservationFilter != null) {
            ServerHttpObservationFilter.findObservationContext(request)
                .ifPresent(context -> tenantObservationFilter.enrich(context, tenantIdentifier));
        }

        request.setAttribute(TENANT_IDENTIFIER_ATTRIBUTE, tenantIdentifier);
        return tenantIdentifier;
    }

    /**
     * Returns a form of the given value that is safe to log: control characters are
     * replaced, and the length is capped, so that an attacker-controlled identifier can
     * neither inject log lines nor flood the log.
     */
    static String sanitizeForLogging(String value) {
        var length = Math.min(value.length(), LOG_MAX_LENGTH);
        var builder = new StringBuilder(length + 1);
        for (int i = 0; i < length; i++) {
            var c = value.charAt(i);
            builder.append(Character.isISOControl(c) ? LOG_REPLACEMENT_CHAR : c);
        }
        if (value.length() > LOG_MAX_LENGTH) {
            builder.append('…');
        }
        return builder.toString();
    }

    /**
     * The exception message describes the violated rule to the caller. An exception
     * without one falls back to a generic description of the outcome.
     */
    private static String messageOf(TenantVerificationException exception, String fallback) {
        var message = exception.getMessage();
        return StringUtils.hasText(message) ? message : fallback;
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
            var identifier = tenantIdentifier != null ? " '" + sanitizeForLogging(tenantIdentifier) + "'" : "";
            logger.debug("No tenant context established for request to %s (%s%s): %s".formatted(request.getRequestURI(),
                    outcome.name().toLowerCase(java.util.Locale.ROOT), identifier, message));
        }
        if (request.getAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE) != null) {
            filterChain.doFilter(request, response);
            return;
        }
        failureHandler.handle(request, response, new TenantResolutionFailure(outcome, message, cause));
    }

    /**
     * A CORS preflight request is skipped, since browsers send it without the
     * credentials, cookies, or custom headers that would carry the tenant.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return CorsUtils.isPreFlightRequest(request) || ignorePathMatcher.matches(request);
    }

    /**
     * The tenant context is re-established on an ASYNC dispatch, so that the response of
     * an asynchronous request is completed with the same tenant as the request.
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
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

        @Nullable
        private TenantResolver<HttpServletRequest> tenantResolver;

        private Set<String> ignorePaths = Set.of();

        @Nullable
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
        public Builder tenantResolver(TenantResolver<HttpServletRequest> tenantResolver) {
            this.tenantResolver = tenantResolver;
            return this;
        }

        /**
         * Path patterns of the requests for which no tenant context is established,
         * matched against the path within the application.
         */
        public Builder ignorePaths(Set<String> ignorePaths) {
            this.ignorePaths = ignorePaths;
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
            Assert.notNull(tenantResolver, "tenantResolver cannot be null");
            Assert.notNull(eventPublisher, "eventPublisher cannot be null");
            return new TenantContextFilter(order, tenantResolver, ignorePaths, eventPublisher,
                    tenantIdentifierValidator, tenantVerifier, tenantObservationFilter, failureHandler);
        }

    }

}
