package io.arconia.opentelemetry.testcontainers;

/**
 * Images used in tests.
 */
public final class Images {

    public static final String LGTM = "grafana/otel-lgtm:0.24.0";

    public static final String OTEL_COLLECTOR = "otel/opentelemetry-collector-contrib:0.149.0";

    public static final String PHOENIX = "arizephoenix/phoenix:version-14.2-nonroot";

    private Images() {
    }

}
