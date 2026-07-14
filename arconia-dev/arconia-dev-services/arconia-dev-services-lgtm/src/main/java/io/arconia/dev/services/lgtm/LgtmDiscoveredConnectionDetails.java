package io.arconia.dev.services.lgtm;

import io.arconia.dev.services.core.registration.DiscoveredContainer;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.otlp.OtlpLoggingConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp.OtlpMetricsConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingConnectionDetails;

/**
 * OTLP connection details for all signals to connect to a shared Grafana LGTM dev service
 * running in a container discovered from another application.
 */
final class LgtmDiscoveredConnectionDetails
        implements OtlpTracingConnectionDetails, OtlpMetricsConnectionDetails, OtlpLoggingConnectionDetails {

    private final String httpUrl;

    private final String grpcUrl;

    LgtmDiscoveredConnectionDetails(DiscoveredContainer container) {
        this.httpUrl = "http://%s:%d".formatted(container.host(), container.mappedPort(DEFAULT_HTTP_PORT));
        this.grpcUrl = "http://%s:%d".formatted(container.host(), container.mappedPort(DEFAULT_GRPC_PORT));
    }

    @Override
    public String getTracesUrl(Protocol protocol) {
        return switch (protocol) {
            case HTTP_PROTOBUF -> httpUrl + TRACES_PATH;
            case GRPC -> grpcUrl;
        };
    }

    @Override
    public String getMetricsUrl(Protocol protocol) {
        return switch (protocol) {
            case HTTP_PROTOBUF -> httpUrl + METRICS_PATH;
            case GRPC -> grpcUrl;
        };
    }

    @Override
    public String getLogsUrl(Protocol protocol) {
        return switch (protocol) {
            case HTTP_PROTOBUF -> httpUrl + LOGS_PATH;
            case GRPC -> grpcUrl;
        };
    }

}
