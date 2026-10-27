package io.arconia.observation.opentelemetry.instrumentation.http;

import java.util.ArrayList;
import java.util.List;

/**
 * Options for OpenTelemetry HTTP semantic conventions.
 */
public class OpenTelemetryHttpOptions {

    /**
     * Whether to capture the URL query string of HTTP server requests as a high-cardinality attribute.
     * HTTP client requests always include it in the full URL.
     */
    private boolean captureUrlQuery = false;

    /**
     * Query parameters whose values are replaced by 'REDACTED' in the URL attributes of HTTP server and client requests,
     * matched case-sensitively.
     * Setting it replaces the default list. An empty list disables the redaction.
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
