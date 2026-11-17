package io.arconia.opentelemetry.autoconfigure.observations;

import java.util.List;
import java.util.Set;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationPredicate;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Disables the observations of HTTP server requests whose path matches one of the
 * ignore paths, including any other observation started while handling the request.
 * <p>
 * Disabling only the HTTP server observation is not enough: observations started while
 * handling the request, such as the Spring Security or JDBC ones, would have no parent
 * and become separate traces. The HTTP server observation carries the request in its
 * context. Any other observation finds the request bound to the current thread by
 * Spring Boot's {@code RequestContextFilter}, which runs before the Spring Security
 * filter chain.
 * <p>
 * Spring Boot has no property for this (spring-boot#34801), and the Spring Security
 * observations don't carry the request (spring-security#12854). Requests on a separate
 * management port are not covered, since Spring Boot doesn't apply the servlet filters
 * to the management context (spring-boot#31811).
 *
 * @see <a href="https://github.com/spring-projects/spring-boot/issues/34801">spring-boot#34801</a>
 * @see <a href="https://github.com/spring-projects/spring-security/issues/12854">spring-security#12854</a>
 * @see <a href="https://github.com/spring-projects/spring-boot/issues/31811">spring-boot#31811</a>
 */
final class IgnorePathsObservationPredicate implements ObservationPredicate {

    private static final Logger logger = LoggerFactory.getLogger(IgnorePathsObservationPredicate.class);

    private static final String IGNORED_ATTRIBUTE = IgnorePathsObservationPredicate.class.getName() + ".IGNORED";

    private final List<PathPattern> ignorePathPatterns;

    IgnorePathsObservationPredicate(Set<String> ignorePathPatterns) {
        Assert.notNull(ignorePathPatterns, "ignorePathPatterns cannot be null");
        this.ignorePathPatterns = ignorePathPatterns.stream().map(IgnorePathsObservationPredicate::parse).toList();
    }

    @Override
    public boolean test(String name, Observation.Context context) {
        if (ignorePathPatterns.isEmpty()) {
            return true;
        }
        HttpServletRequest request = currentRequest(context);
        return request == null || !isIgnored(request);
    }

    /**
     * Returns the HTTP request the observation belongs to: the one in the HTTP server
     * observation context, or else the one bound to the current thread.
     */
    private static @Nullable HttpServletRequest currentRequest(Observation.Context context) {
        if (context instanceof ServerRequestObservationContext serverContext && serverContext.getCarrier() != null) {
            return serverContext.getCarrier();
        }
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    /**
     * Matches the request once and stores the decision on it, since the predicate runs
     * for every observation started while handling the request, and the decision carries
     * over to the error dispatch of the same request.
     */
    private boolean isIgnored(HttpServletRequest request) {
        if (request.getAttribute(IGNORED_ATTRIBUTE) instanceof Boolean ignored) {
            return ignored;
        }
        boolean ignored = matches(request);
        request.setAttribute(IGNORED_ATTRIBUTE, ignored);
        return ignored;
    }

    private boolean matches(HttpServletRequest request) {
        var requestUri = requestUri(request);
        // Matched against the path within the application, so that the configured
        // patterns keep working under a non-root context path. The path is left encoded,
        // since decoding first would let an escaped sequence match a pattern differently
        // than the container routes it.
        var pathContainer = RequestPath.parse(requestUri, request.getContextPath()).pathWithinApplication();
        var matchesIgnorePaths = ignorePathPatterns.stream()
            .anyMatch(pathPattern -> pathPattern.matches(pathContainer));
        if (matchesIgnorePaths) {
            logger.debug("Request '{}' matches one of the paths to ignore when recording observations", requestUri);
        }
        return matchesIgnorePaths;
    }

    /**
     * Returns the URI the client requested. On an error dispatch, the request URI is
     * the error page, and the original one is in a request attribute.
     */
    private static String requestUri(HttpServletRequest request) {
        if (request.getDispatcherType() == DispatcherType.ERROR
                && request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI) instanceof String errorRequestUri) {
            return errorRequestUri;
        }
        return request.getRequestURI();
    }

    private static PathPattern parse(String pattern) {
        var parser = PathPatternParser.defaultInstance;
        return parser.parse(parser.initFullPathPattern(pattern));
    }

}
