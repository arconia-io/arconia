package io.arconia.dev.ui.server.web;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.FilterChain;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

import io.arconia.core.annotations.Internal;
import io.arconia.dev.ui.server.web.DevUiHosts.Rejection;

/**
 * The entry point of the Dev UI: it rejects the requests the Dev UI doesn't accept, and hands
 * the accepted ones over to the servlet that serves the Dev UI.
 * <p>
 * An accepted request goes straight to the servlet and not through the filters that come after
 * this one. The Dev UI is then available whatever the application does with its requests:
 * Spring Security asking for a login, a filter resolving the current tenant, or any other
 * filter of the application that would reject a request it knows nothing about.
 */
@Internal
public final class DevUiFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(DevUiFilter.class);

    private static final int MAX_REPORTED_REJECTIONS = 50;

    private static final String CONTENT_TYPE_OPTIONS_HEADER = "X-Content-Type-Options";

    private final Set<Rejection> reportedRejections = ConcurrentHashMap.newKeySet();

    private final Servlet servlet;

    private final DevUiHosts hosts;

    public DevUiFilter(Servlet servlet, DevUiHosts hosts) {
        Assert.notNull(servlet, "servlet cannot be null");
        Assert.notNull(hosts, "hosts cannot be null");
        this.servlet = servlet;
        this.hosts = hosts;
    }

    /**
     * A request that reaches the Dev UI as the error page of another one, or again after it
     * was handled asynchronously, goes through the same checks.
     */
    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException {
        // Every answer is read as the type it declares, so that a browser never reads one
        // that echoes part of the request as a page of its own.
        response.setHeader(CONTENT_TYPE_OPTIONS_HEADER, "nosniff");
        Rejection rejection = this.hosts.rejection(request);
        if (rejection != null) {
            warnAbout(rejection);
            DevUiResponses.writeStatus(response, HttpServletResponse.SC_FORBIDDEN, "The Dev UI only accepts requests for the local machine");
            return;
        }
        if (!HttpMethod.GET.matches(request.getMethod()) && !HttpMethod.HEAD.matches(request.getMethod())) {
            Rejection writeRejection = this.hosts.writeRejection(request);
            if (writeRejection != null) {
                warnAbout(writeRejection);
                DevUiResponses.writeStatus(response, HttpServletResponse.SC_FORBIDDEN,
                        "The Dev UI only accepts a request that changes something from the Dev UI itself");
                return;
            }
        }
        try {
            // The container has already matched the request to the servlet, so the servlet sees
            // the same request as if it had come through the rest of the filters.
            this.servlet.service(request, response);
        } catch (IOException ex) {
            // The response could not be written, which is what happens when the browser goes
            // away before it has all of it, as when the developer moves to another page. That
            // is not a failure worth a line in the logs of the application.
            logger.debug("The response to the Dev UI request '{}' could not be written", request.getRequestURI(), ex);
            if (!response.isCommitted()) {
                response.reset();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (ServletException | RuntimeException | LinkageError ex) {
            // A failure that leaves this filter would be handled by the application, which
            // would answer with an error page of its own or ask for a login.
            logger.error("The Dev UI failed to handle the request '{}'", request.getRequestURI(), ex);
            if (!response.isCommitted()) {
                response.reset();
                DevUiResponses.writeStatus(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "The Dev UI failed to handle the request. See the application log for the reason.");
            }
        }
    }

    /**
     * Warns once about each rejected host, origin, client, or kind of request, so that a web
     * page sending requests in a loop cannot fill the application log.
     */
    private void warnAbout(Rejection rejection) {
        if (this.reportedRejections.size() < MAX_REPORTED_REJECTIONS && this.reportedRejections.add(rejection)) {
            logger.warn("Rejected a Dev UI request: {}", rejection.message());
        }
    }

}
