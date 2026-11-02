package io.arconia.dev.services.opentelemetry.collector;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.ssl.SslBundle;

import io.arconia.dev.services.core.registration.DiscoveredContainer;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.otlp.OtlpLoggingConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp.OtlpMetricsConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingConnectionDetails;

/**
 * OTLP connection details for all signals to connect to an OpenTelemetry Collector
 * dev service running in a container discovered from another application.
 */
final class OtelCollectorDiscoveredConnectionDetails
        implements OtlpTracingConnectionDetails, OtlpMetricsConnectionDetails, OtlpLoggingConnectionDetails {

    private final String httpUrl;

    private final String grpcUrl;

    @Nullable
    private final SslBundle sslBundle;

    OtelCollectorDiscoveredConnectionDetails(DiscoveredContainer container) {
        this.sslBundle = container.sslBundle();
        String scheme = (sslBundle != null) ? "https" : "http";
        this.httpUrl = "%s://%s:%d".formatted(scheme, container.host(), container.mappedPort(DEFAULT_HTTP_PORT));
        this.grpcUrl = "%s://%s:%d".formatted(scheme, container.host(), container.mappedPort(DEFAULT_GRPC_PORT));
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

    @Override
    @Nullable
    public SslBundle getSslBundle() {
        return sslBundle;
    }

}
