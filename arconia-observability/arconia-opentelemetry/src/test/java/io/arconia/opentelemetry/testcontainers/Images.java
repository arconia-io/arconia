package io.arconia.opentelemetry.testcontainers;

/**
 * Images used in tests.
 */
public final class Images {

    public static final String LGTM = "grafana/otel-lgtm:0.18.1";

    public static final String OTEL_COLLECTOR = "otel/opentelemetry-collector-contrib:0.146.1";

    public static final String PHOENIX = "arizephoenix/phoenix:version-13.2-nonroot";

    private Images() {
    }

}
