package io.arconia.observation.opentelemetry.instrumentation.http;

/**
 * Options for OpenTelemetry HTTP semantic conventions.
 */
public class OpenTelemetryHttpOptions {

    /**
     * Whether to include the URL query string as a high-cardinality attribute.
     */
    private boolean includeUrlQuery = false;

    public boolean isIncludeUrlQuery() {
        return includeUrlQuery;
    }

    public void setIncludeUrlQuery(boolean includeUrlQuery) {
        this.includeUrlQuery = includeUrlQuery;
    }

}
