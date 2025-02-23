package io.arconia.dev.service.connections;

import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.testcontainers.grafana.LgtmStackContainer;

import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp.OtlpMetricsConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp.OtlpMetricsExporterAutoConfiguration;

/**
 * Factory for creating {@link OtlpMetricsConnectionDetails} for LGTM containers.
 */
public class LgtmOtlpMetricsContainerConnectionDetailsFactory
        extends ContainerConnectionDetailsFactory<LgtmStackContainer, OtlpMetricsConnectionDetails> {

    LgtmOtlpMetricsContainerConnectionDetailsFactory() {
        super(ANY_CONNECTION_NAME, OtlpMetricsExporterAutoConfiguration.class.getName());
    }

    @Override
    protected OtlpMetricsConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<LgtmStackContainer> source) {
        return new LgtmOtlpMetricsContainerConnectionDetails(source);
    }

    private static final class LgtmOtlpMetricsContainerConnectionDetails extends ContainerConnectionDetails<LgtmStackContainer> implements OtlpMetricsConnectionDetails {
        private LgtmOtlpMetricsContainerConnectionDetails(ContainerConnectionSource<LgtmStackContainer> source) {
            super(source);
        }

        @Override
        public String getUrl(Protocol protocol) {
            String url = switch (protocol) {
                case HTTP_PROTOBUF -> getContainer().getOtlpHttpUrl();
                case GRPC -> getContainer().getOtlpGrpcUrl();
            };
            return protocol == Protocol.HTTP_PROTOBUF ? "%s/v1/metrics".formatted(url) : url;
        }
    }

}
