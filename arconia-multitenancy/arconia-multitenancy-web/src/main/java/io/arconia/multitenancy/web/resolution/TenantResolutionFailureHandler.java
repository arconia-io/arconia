package io.arconia.multitenancy.web.resolution;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.arconia.core.support.Incubating;

/**
 * Strategy used to handle an HTTP request for which no tenant context could be
 * established, for example, by writing an error response or redirecting the client.
 */
@Incubating
@FunctionalInterface
public interface TenantResolutionFailureHandler {

    /**
     * Handles the given failure by completing the response.
     */
    void handle(HttpServletRequest request, HttpServletResponse response, TenantResolutionFailure failure)
            throws IOException, ServletException;

}
