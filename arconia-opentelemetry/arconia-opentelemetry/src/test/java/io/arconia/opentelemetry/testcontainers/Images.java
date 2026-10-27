package io.arconia.opentelemetry.testcontainers;

/**
 * Images used in tests.
 */
public final class Images {

    public static final String CLICKHOUSE = "clickhouse/clickhouse-server:26.10-distroless";

    public static final String LGTM = "grafana/otel-lgtm:0.36.0";

    public static final String OTEL_COLLECTOR = "otel/opentelemetry-collector-contrib:0.162.0";

    public static final String OPENLIT = "ghcr.io/openlit/openlit:2.1.1";

    public static final String PHOENIX = "arizephoenix/phoenix:version-20.21-nonroot";

    private Images() {
    }

}
