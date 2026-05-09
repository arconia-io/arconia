package io.arconia.opentelemetry.testcontainers;

import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;

import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp.OtlpMetricsConnectionDetails;
import io.arconia.testcontainers.openlit.OpenLitContainer;

/**
 * Factory for creating {@link OtlpMetricsConnectionDetails} for OpenLit containers.
 */
class OpenLitOtlpMetricsContainerConnectionDetailsFactory
        extends ContainerConnectionDetailsFactory<OpenLitContainer, OtlpMetricsConnectionDetails> {

    OpenLitOtlpMetricsContainerConnectionDetailsFactory() {
        super(ANY_CONNECTION_NAME);
    }

    @Override
    protected OtlpMetricsConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<OpenLitContainer> source) {
        return new OpenLitOtlpMetricsContainerConnectionDetails(source);
    }

    private static final class OpenLitOtlpMetricsContainerConnectionDetails
            extends ContainerConnectionDetails<OpenLitContainer> implements OtlpMetricsConnectionDetails {

        private OpenLitOtlpMetricsContainerConnectionDetails(ContainerConnectionSource<OpenLitContainer> source) {
            super(source);
        }

        @Override
        public String getUrl(Protocol protocol) {
            return switch (protocol) {
                case HTTP_PROTOBUF ->
                        "http://%s:%d%s".formatted(getContainer().getHost(), getContainer().getOtlpHttpPort(), METRICS_PATH);
                case GRPC ->
                        "http://%s:%d".formatted(getContainer().getHost(), getContainer().getOtlpGrpcPort());
            };
        }

    }

}
