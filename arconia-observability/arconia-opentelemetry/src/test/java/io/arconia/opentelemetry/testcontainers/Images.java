package io.arconia.opentelemetry.testcontainers;

/**
 * Images used in tests.
 */
public final class Images {

    public static final String LGTM = "grafana/otel-lgtm:0.26.0";

    public static final String OTEL_COLLECTOR = "otel/opentelemetry-collector-contrib:0.149.0";

    public static final String PHOENIX = "arizephoenix/phoenix:version-14.12.0-nonroot";

    private Images() {
    }

}
