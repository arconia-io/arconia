package io.arconia.opentelemetry.testcontainers;

import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.testcontainers.containers.Container;

import io.arconia.opentelemetry.autoconfigure.exporter.otlp.OtlpConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.otlp.OtlpLoggingConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp.OtlpMetricsConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingConnectionDetails;

/**
 * Factory for creating {@link OtlpConnectionDetails} for all OpenTelemetry signals
 * for OpenTelemetry Collector containers.
 */
class OtelCollectorOtlpContainerConnectionDetailsFactory
        extends ContainerConnectionDetailsFactory<Container<?>, OtlpConnectionDetails> {

    private static final String CONNECTION_NAME = "otel/opentelemetry-collector-contrib";

    OtelCollectorOtlpContainerConnectionDetailsFactory() {
        super(CONNECTION_NAME);
    }

    @Override
    protected OtlpConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<Container<?>> source) {
        return new OtelCollectorOtlpContainerConnectionDetails(source);
    }

    private static final class OtelCollectorOtlpContainerConnectionDetails extends ContainerConnectionDetails<Container<?>>
            implements OtlpTracingConnectionDetails, OtlpMetricsConnectionDetails, OtlpLoggingConnectionDetails {

        private OtelCollectorOtlpContainerConnectionDetails(ContainerConnectionSource<Container<?>> source) {
            super(source);
        }

        @Override
        public String getTracesUrl(Protocol protocol) {
            return switch (protocol) {
                case HTTP_PROTOBUF -> httpUrl() + TRACES_PATH;
                case GRPC -> grpcUrl();
            };
        }

        @Override
        public String getMetricsUrl(Protocol protocol) {
            return switch (protocol) {
                case HTTP_PROTOBUF -> httpUrl() + METRICS_PATH;
                case GRPC -> grpcUrl();
            };
        }

        @Override
        public String getLogsUrl(Protocol protocol) {
            return switch (protocol) {
                case HTTP_PROTOBUF -> httpUrl() + LOGS_PATH;
                case GRPC -> grpcUrl();
            };
        }

        private String httpUrl() {
            return "http://%s:%d".formatted(getContainer().getHost(), getContainer().getMappedPort(DEFAULT_HTTP_PORT));
        }

        private String grpcUrl() {
            return "http://%s:%d".formatted(getContainer().getHost(), getContainer().getMappedPort(DEFAULT_GRPC_PORT));
        }

    }

}
