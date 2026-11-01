package io.arconia.observation.opentelemetry.instrumentation.http;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.opentelemetry.semconv.AttributeKeyTemplate;
import io.opentelemetry.semconv.HttpAttributes;

/**
 * Options for OpenTelemetry HTTP semantic conventions.
 */
public class OpenTelemetryHttpOptions {

    /**
     * Whether to capture the URL query string of the HTTP server observations, with the values
     * of the sensitive query parameters redacted. The HTTP client observations always include it in "url.full".
     */
    private boolean captureUrlQuery = true;

    /**
     * Query parameters whose values are replaced by REDACTED in the URL query string of the HTTP server and client
     * observations, matched case-sensitively. Setting it replaces the default list, and an empty list disables the redaction.
     */
    private List<String> sensitiveQueryParameters = new ArrayList<>(List.of("AWSAccessKeyId", "Signature",
            "X-Amz-Signature", "X-Amz-Credential", "X-Amz-Security-Token", "sig", "X-Goog-Signature"));

    /**
     * Additional query parameters whose values are replaced by REDACTED in the URL query string of the HTTP server
     * and client observations, on top of the sensitive query parameters, matched case-sensitively.
     */
    private List<String> additionalSensitiveQueryParameters = new ArrayList<>();

    /**
     * HTTP request headers to record on the HTTP server and client spans, matched case-insensitively.
     * Avoid headers carrying sensitive information, such as Authorization.
     */
    private List<String> captureRequestHeaders = new ArrayList<>();

    /**
     * HTTP response headers to record on the HTTP server and client spans, matched case-insensitively.
     * Avoid headers carrying sensitive information, such as Set-Cookie.
     */
    private List<String> captureResponseHeaders = new ArrayList<>();

    public boolean isCaptureUrlQuery() {
        return captureUrlQuery;
    }

    public void setCaptureUrlQuery(boolean captureUrlQuery) {
        this.captureUrlQuery = captureUrlQuery;
    }

    public List<String> getSensitiveQueryParameters() {
        return sensitiveQueryParameters;
    }

    public void setSensitiveQueryParameters(List<String> sensitiveQueryParameters) {
        this.sensitiveQueryParameters = sensitiveQueryParameters;
    }

    public List<String> getAdditionalSensitiveQueryParameters() {
        return additionalSensitiveQueryParameters;
    }

    public void setAdditionalSensitiveQueryParameters(List<String> additionalSensitiveQueryParameters) {
        this.additionalSensitiveQueryParameters = additionalSensitiveQueryParameters;
    }

    public List<String> getCaptureRequestHeaders() {
        return captureRequestHeaders;
    }

    public void setCaptureRequestHeaders(List<String> captureRequestHeaders) {
        this.captureRequestHeaders = captureRequestHeaders;
    }

    public List<String> getCaptureResponseHeaders() {
        return captureResponseHeaders;
    }

    public void setCaptureResponseHeaders(List<String> captureResponseHeaders) {
        this.captureResponseHeaders = captureResponseHeaders;
    }

    /**
     * Returns the request headers to capture, each mapped to its {@code http.request.header.<name>} attribute key.
     */
    Map<String, String> requestHeaderAttributeKeys() {
        return headerAttributeKeys(captureRequestHeaders, HttpAttributes.HTTP_REQUEST_HEADER);
    }

    /**
     * Returns the response headers to capture, each mapped to its {@code http.response.header.<name>} attribute key.
     */
    Map<String, String> responseHeaderAttributeKeys() {
        return headerAttributeKeys(captureResponseHeaders, HttpAttributes.HTTP_RESPONSE_HEADER);
    }

    /**
     * Maps each header name, in lower case as the conventions require, to its attribute key.
     */
    private static Map<String, String> headerAttributeKeys(List<String> headers, AttributeKeyTemplate<?> template) {
        Map<String, String> attributeKeys = new LinkedHashMap<>();
        for (String header : headers) {
            String name = header.toLowerCase(Locale.ROOT);
            attributeKeys.put(name, template.getAttributeKey(name).getKey());
        }
        return Map.copyOf(attributeKeys);
    }

    /**
     * Returns the query parameters to redact: the sensitive ones and the additional ones.
     * It isn't a getter, so that it isn't bound as a configuration property.
     */
    List<String> queryParametersToRedact() {
        List<String> parameters = new ArrayList<>(sensitiveQueryParameters);
        parameters.addAll(additionalSensitiveQueryParameters);
        return List.copyOf(parameters);
    }

}
