package io.arconia.opentelemetry.testcontainers;

import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;

import io.arconia.opentelemetry.autoconfigure.exporter.otlp.OtlpConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.otlp.OtlpLoggingConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp.OtlpMetricsConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingConnectionDetails;
import io.arconia.testcontainers.openlit.OpenLitContainer;

/**
 * Factory for creating {@link OtlpConnectionDetails} for all OpenTelemetry signals
 * for OpenLit containers.
 */
class OpenLitOtlpContainerConnectionDetailsFactory
        extends ContainerConnectionDetailsFactory<OpenLitContainer, OtlpConnectionDetails> {

    OpenLitOtlpContainerConnectionDetailsFactory() {
        super(ANY_CONNECTION_NAME);
    }

    @Override
    protected OtlpConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<OpenLitContainer> source) {
        return new OpenLitOtlpContainerConnectionDetails(source);
    }

    private static final class OpenLitOtlpContainerConnectionDetails extends ContainerConnectionDetails<OpenLitContainer>
            implements OtlpTracingConnectionDetails, OtlpMetricsConnectionDetails, OtlpLoggingConnectionDetails {

        private OpenLitOtlpContainerConnectionDetails(ContainerConnectionSource<OpenLitContainer> source) {
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
            return "http://%s:%d".formatted(getContainer().getHost(), getContainer().getOtlpHttpPort());
        }

        private String grpcUrl() {
            return "http://%s:%d".formatted(getContainer().getHost(), getContainer().getOtlpGrpcPort());
        }

    }

}
