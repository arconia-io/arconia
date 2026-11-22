package io.arconia.opentelemetry.testcontainers;

/**
 * Images used in tests.
 */
public final class Images {

    public static final String CLICKHOUSE = "clickhouse/clickhouse-server:26.10-distroless";

    public static final String FLOCI = "floci/floci:2.5.0";

    public static final String LANGFUSE = "langfuse/langfuse:4.83";

    public static final String LANGFUSE_WORKER = "langfuse/langfuse-worker:4.83";

    public static final String LGTM = "grafana/otel-lgtm:0.41.1";

    public static final String OTEL_COLLECTOR = "otel/opentelemetry-collector-contrib:0.165.0";

    public static final String OPENLIT = "ghcr.io/openlit/openlit:2.2.0";

    public static final String POSTGRES = "postgres:18.7-alpine";

    public static final String PHOENIX = "arizephoenix/phoenix:version-22.0-nonroot";

    public static final String REDIS = "redis:8.10-alpine";

    private Images() {
    }

}
