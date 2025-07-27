package io.arconia.opentelemetry.testcontainers;

import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.testcontainers.grafana.LgtmStackContainer;

import io.arconia.core.support.Internal;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp.OtlpMetricsConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp.OtlpMetricsExporterConfiguration;

/**
 * Factory for creating {@link OtlpMetricsConnectionDetails} for LGTM containers.
 */
@Internal
class LgtmOpenTelemetryMetricsContainerConnectionDetailsFactory
        extends ContainerConnectionDetailsFactory<LgtmStackContainer, OtlpMetricsConnectionDetails> {

    LgtmOpenTelemetryMetricsContainerConnectionDetailsFactory() {
        super(ANY_CONNECTION_NAME, OtlpMetricsExporterConfiguration.class.getName());
    }

    @Override
    protected OtlpMetricsConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<LgtmStackContainer> source) {
        return new LgtmOpenTelemetryMetricsContainerConnectionDetails(source);
    }

    private static final class LgtmOpenTelemetryMetricsContainerConnectionDetails extends ContainerConnectionDetails<LgtmStackContainer> implements OtlpMetricsConnectionDetails {
        private LgtmOpenTelemetryMetricsContainerConnectionDetails(ContainerConnectionSource<LgtmStackContainer> source) {
            super(source);
        }

        @Override
        public String getUrl(Protocol protocol) {
            String url = switch (protocol) {
                case HTTP_PROTOBUF -> getContainer().getOtlpHttpUrl();
                case GRPC -> getContainer().getOtlpGrpcUrl();
            };
            return protocol == Protocol.HTTP_PROTOBUF ? url + METRICS_PATH : url;
        }
    }

}
