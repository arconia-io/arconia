package io.arconia.docling.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import io.arconia.docling.autoconfigure.DoclingProperties;

/**
 * Logs the requests sent to the Docling Serve API and the responses received from it,
 * masking the API key and any Authorization header.
 * <p>
 * Response bodies are only readable when the {@code RestClient} buffers them.
 */
final class LoggingClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(DoclingServeClient.class);

    private static final String BINARY_BODY = "<binary>";

    private final boolean logRequests;
    private final boolean logResponses;
    private final boolean prettyPrint;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    LoggingClientHttpRequestInterceptor(boolean logRequests, boolean logResponses, boolean prettyPrint) {
        this.logRequests = logRequests;
        this.logResponses = logResponses;
        this.prettyPrint = prettyPrint;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        if (logRequests) {
            logRequest(request, body);
        }
        ClientHttpResponse response = execution.execute(request, body);
        if (logResponses) {
            logResponse(response);
        }
        return response;
    }

    private void logRequest(HttpRequest request, byte[] body) {
        logger.info("Request:\n- method: {}\n- url: {}\n- headers: {}\n- body: {}",
                request.getMethod(), request.getURI(), headers(request.getHeaders()),
                body(request.getHeaders().getContentType(), new String(body, StandardCharsets.UTF_8)));
    }

    private void logResponse(ClientHttpResponse response) throws IOException {
        MediaType contentType = response.getHeaders().getContentType();
        String body = isJson(contentType) ? StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8) : BINARY_BODY;
        logger.info("Response:\n- status code: {}\n- headers: {}\n- body: {}",
                response.getStatusCode().value(), headers(response.getHeaders()), body(contentType, body));
    }

    private String body(@Nullable MediaType contentType, String body) {
        if (!prettyPrint || body.isBlank() || !isJson(contentType)) {
            return body;
        }
        try {
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonMapper.readTree(body));
        } catch (JacksonException ex) {
            return body;
        }
    }

    private static boolean isJson(@Nullable MediaType contentType) {
        return contentType != null && MediaType.APPLICATION_JSON.isCompatibleWith(contentType);
    }

    private static String headers(HttpHeaders headers) {
        return headers.headerSet().stream()
                .map(header -> "[%s: %s]".formatted(header.getKey(), String.join(", ", header.getValue().stream()
                        .map(value -> shouldMask(header.getKey()) ? mask(value) : value)
                        .toList())))
                .collect(Collectors.joining(", "));
    }

    private static boolean shouldMask(String headerName) {
        return HttpHeaders.AUTHORIZATION.equalsIgnoreCase(headerName)
                || DoclingProperties.API_KEY_HEADER_NAME.equalsIgnoreCase(headerName);
    }

    private static String mask(String value) {
        if (value.length() <= 4) {
            return value;
        }
        return value.substring(0, 2) + "..." + value.substring(value.length() - 2);
    }

}
