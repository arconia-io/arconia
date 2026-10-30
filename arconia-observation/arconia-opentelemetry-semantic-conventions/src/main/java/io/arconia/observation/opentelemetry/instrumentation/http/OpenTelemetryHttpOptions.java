package io.arconia.observation.opentelemetry.instrumentation.http;

import java.util.ArrayList;
import java.util.List;

/**
 * Options for OpenTelemetry HTTP semantic conventions.
 */
public class OpenTelemetryHttpOptions {

    /**
     * Whether to capture the URL query string of the HTTP server observations as url.query, with the values
     * of the sensitive query parameters redacted. The HTTP client observations always include it in url.full.
     */
    private boolean captureUrlQuery = true;

    /**
     * Query parameters whose values are replaced by REDACTED in the URL query string of the HTTP server and client
     * observations, matched case-sensitively. Setting it replaces the default list, and an empty list disables the redaction.
     */
    private List<String> sensitiveQueryParameters = new ArrayList<>(List.of("AWSAccessKeyId", "Signature",
            "X-Amz-Signature", "X-Amz-Credential", "X-Amz-Security-Token", "sig", "X-Goog-Signature"));

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

}
