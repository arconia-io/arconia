package io.arconia.dev.service.connections;

import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.testcontainers.grafana.LgtmStackContainer;

import io.arconia.opentelemetry.autoconfigure.sdk.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.sdk.logs.exporter.otlp.OtlpLoggingConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.sdk.logs.exporter.otlp.OtlpLoggingExporterAutoConfiguration;

/**
 * Factory for creating {@link OtlpLoggingConnectionDetails} for LGTM containers.
 */
public class LgtmOtlpLoggingContainerConnectionDetailsFactory
        extends ContainerConnectionDetailsFactory<LgtmStackContainer, OtlpLoggingConnectionDetails> {

    LgtmOtlpLoggingContainerConnectionDetailsFactory() {
        super(ANY_CONNECTION_NAME, OtlpLoggingExporterAutoConfiguration.class.getName());
    }

    @Override
    protected OtlpLoggingConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<LgtmStackContainer> source) {
        return new LgtmOtlpLoggingContainerConnectionDetails(source);
    }

    private static final class LgtmOtlpLoggingContainerConnectionDetails extends ContainerConnectionDetails<LgtmStackContainer> implements OtlpLoggingConnectionDetails {
        private LgtmOtlpLoggingContainerConnectionDetails(ContainerConnectionSource<LgtmStackContainer> source) {
            super(source);
        }

        @Override
        public String getUrl(Protocol protocol) {
            String url = switch (protocol) {
                case HTTP_PROTOBUF -> getContainer().getOtlpHttpUrl();
                case GRPC -> getContainer().getOtlpGrpcUrl();
            };
            return protocol == Protocol.HTTP_PROTOBUF ? "%s/v1/logs".formatted(url) : url;
        }
    }

}
