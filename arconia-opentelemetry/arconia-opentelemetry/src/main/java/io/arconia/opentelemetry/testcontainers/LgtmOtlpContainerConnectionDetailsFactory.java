package io.arconia.opentelemetry.testcontainers;

import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.testcontainers.grafana.LgtmStackContainer;

import io.arconia.opentelemetry.autoconfigure.exporter.otlp.OtlpConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.otlp.OtlpLoggingConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp.OtlpMetricsConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingConnectionDetails;

/**
 * Factory for creating {@link OtlpConnectionDetails} for all OpenTelemetry signals
 * for Grafana LGTM containers.
 */
class LgtmOtlpContainerConnectionDetailsFactory
        extends ContainerConnectionDetailsFactory<LgtmStackContainer, OtlpConnectionDetails> {

    LgtmOtlpContainerConnectionDetailsFactory() {
        super(ANY_CONNECTION_NAME);
    }

    @Override
    protected OtlpConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<LgtmStackContainer> source) {
        return new LgtmOtlpContainerConnectionDetails(source);
    }

    private static final class LgtmOtlpContainerConnectionDetails extends ContainerConnectionDetails<LgtmStackContainer>
            implements OtlpTracingConnectionDetails, OtlpMetricsConnectionDetails, OtlpLoggingConnectionDetails {

        private LgtmOtlpContainerConnectionDetails(ContainerConnectionSource<LgtmStackContainer> source) {
            super(source);
        }

        @Override
        public String getTracesUrl(Protocol protocol) {
            return switch (protocol) {
                case HTTP_PROTOBUF -> getContainer().getOtlpHttpUrl() + TRACES_PATH;
                case GRPC -> getContainer().getOtlpGrpcUrl();
            };
        }

        @Override
        public String getMetricsUrl(Protocol protocol) {
            return switch (protocol) {
                case HTTP_PROTOBUF -> getContainer().getOtlpHttpUrl() + METRICS_PATH;
                case GRPC -> getContainer().getOtlpGrpcUrl();
            };
        }

        @Override
        public String getLogsUrl(Protocol protocol) {
            return switch (protocol) {
                case HTTP_PROTOBUF -> getContainer().getOtlpHttpUrl() + LOGS_PATH;
                case GRPC -> getContainer().getOtlpGrpcUrl();
            };
        }

    }

}
