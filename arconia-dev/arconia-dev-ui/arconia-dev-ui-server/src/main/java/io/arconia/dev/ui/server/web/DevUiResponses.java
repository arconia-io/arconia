package io.arconia.dev.ui.server.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;

/**
 * Writes the responses of the Dev UI that carry no content of their own.
 */
final class DevUiResponses {

    private static final String CONTENT_TYPE = new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8).toString();

    private DevUiResponses() {}

    /**
     * Writes a status with a plain text message as the complete response. Unlike
     * {@code sendError}, it doesn't trigger the error handling of the application, which would
     * replace the response with the error page or the error body the application defines.
     */
    static void writeStatus(HttpServletResponse response, int status, String message) throws IOException {
        byte[] body = message.getBytes(StandardCharsets.UTF_8);
        response.setStatus(status);
        response.setContentType(CONTENT_TYPE);
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
    }

}
