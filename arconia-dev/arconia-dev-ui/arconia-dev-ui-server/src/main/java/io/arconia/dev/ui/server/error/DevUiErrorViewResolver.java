package io.arconia.dev.ui.server.error;

import java.util.List;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.ModelAndView;

import io.arconia.core.annotations.Internal;
import io.arconia.dev.ui.server.error.ApplicationEndpoints.Endpoint;
import io.arconia.dev.ui.server.web.DevUiHosts;

/**
 * Shows the welcome page and the error pages of the Dev UI in place of the whitelabel error
 * page of Spring Boot, and only in place of it.
 * <p>
 * Spring Boot asks the error view resolvers for a page when a browser request fails, and falls
 * back to the whitelabel error page when none of them has one. This resolver steps in at that
 * point: it first asks the other resolvers, and has a page only when none of them does, so
 * that an error page of the application is used first, and only when the whitelabel error
 * page is what the application would show. Spring Boot doesn't define that page when
 * the application turns it off or has an error template of its own.
 * <p>
 * The pages are only shown for the requests the Dev UI accepts, since the page for a failed
 * request includes its stack trace.
 */
@Internal
public final class DevUiErrorViewResolver implements ErrorViewResolver, Ordered {

    /**
     * The name of the view Spring Boot falls back to, which is the whitelabel error page
     * unless the application defines its own.
     */
    private static final String ERROR_VIEW = "error";

    private final ApplicationContext applicationContext;

    private final ObjectProvider<ErrorViewResolver> errorViewResolvers;

    private final ObjectProvider<ErrorAttributes> errorAttributes;

    private final DevUiHosts hosts;

    public DevUiErrorViewResolver(ApplicationContext applicationContext, ObjectProvider<ErrorViewResolver> errorViewResolvers,
            ObjectProvider<ErrorAttributes> errorAttributes, DevUiHosts hosts
    ) {
        this.applicationContext = applicationContext;
        this.errorViewResolvers = errorViewResolvers;
        this.errorAttributes = errorAttributes;
        this.hosts = hosts;
    }

    @Override
    public @Nullable ModelAndView resolveErrorView(HttpServletRequest request, HttpStatus status, Map<String, Object> model) {
        if (!this.hosts.accepts(request) || !isWhitelabelErrorPageInUse() || hasErrorPageOfItsOwn(request, status, model)) {
            return null;
        }
        String contextPath = request.getContextPath();
        String path = path(request);
        if (request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI) == null) {
            // Nothing was sent to the error path: it was requested directly.
            return new ModelAndView(DevUiErrorPage.nothingFailed(path, contextPath));
        }
        if (status != HttpStatus.NOT_FOUND) {
            return new ModelAndView(DevUiErrorPage.error(status, request.getMethod(), path, exception(request), applicationPackages(), contextPath));
        }
        List<Endpoint> endpoints = ApplicationEndpoints.find(this.applicationContext);
        return new ModelAndView(path.equals("/") ? DevUiErrorPage.welcome(endpoints, contextPath)
                : DevUiErrorPage.notFound(path, endpoints, contextPath));
    }

    /**
     * Looks at the view itself and not at its declared type, which says nothing about where
     * the view comes from until it has been created, as with lazy initialization.
     */
    private boolean isWhitelabelErrorPageInUse() {
        if (!this.applicationContext.containsBean(ERROR_VIEW)) {
            return false;
        }
        Class<?> type = this.applicationContext.getBean(ERROR_VIEW).getClass();
        return ErrorMvcAutoConfiguration.class.equals(type.getEnclosingClass());
    }

    /**
     * Whether another resolver has a page for the error, in which case it is left to show it
     * when its turn comes.
     */
    private boolean hasErrorPageOfItsOwn(HttpServletRequest request, HttpStatus status, Map<String, Object> model) {
        return this.errorViewResolvers.orderedStream()
                .filter(resolver -> resolver != this)
                .anyMatch(resolver -> resolver.resolveErrorView(request, status, model) != null);
    }

    /**
     * The path that was requested, without the context path of the application. During the
     * handling of an error, the request itself is the one for the error path.
     */
    private static String path(HttpServletRequest request) {
        Object errorPath = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String path = (errorPath != null) ? errorPath.toString() : request.getRequestURI();
        String contextPath = request.getContextPath();
        if (!contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return path.isEmpty() ? "/" : path;
    }

    /**
     * The packages of the application, which tell its frames apart from the ones of the
     * libraries in a stack trace.
     */
    private List<String> applicationPackages() {
        return AutoConfigurationPackages.has(this.applicationContext) ? AutoConfigurationPackages.get(this.applicationContext) : List.of();
    }

    private @Nullable Throwable exception(HttpServletRequest request) {
        ErrorAttributes errorAttributes = this.errorAttributes.getIfAvailable();
        return (errorAttributes != null) ? errorAttributes.getError(new ServletWebRequest(request)) : null;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 10;
    }

}
