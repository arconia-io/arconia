package io.arconia.opentelemetry.testcontainers;

/**
 * Images used in tests.
 */
public final class Images {

    public static final String CLICKHOUSE = "clickhouse/clickhouse-server:26.5-distroless";

    public static final String LGTM = "grafana/otel-lgtm:0.28.0";

    public static final String OTEL_COLLECTOR = "otel/opentelemetry-collector-contrib:0.153.0";

    public static final String OPENLIT = "ghcr.io/openlit/openlit:1.21.1";

    public static final String PHOENIX = "arizephoenix/phoenix:version-17.2-nonroot";

    private Images() {
    }

}
