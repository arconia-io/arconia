package io.arconia.opentelemetry.testcontainers;

import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.testcontainers.grafana.LgtmStackContainer;

import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.otlp.OtlpLoggingConnectionDetails;

/**
 * Factory for creating {@link OtlpLoggingConnectionDetails} for Grafana LGTM containers.
 */
class LgtmOtlpLoggingContainerConnectionDetailsFactory
        extends ContainerConnectionDetailsFactory<LgtmStackContainer, OtlpLoggingConnectionDetails> {

    LgtmOtlpLoggingContainerConnectionDetailsFactory() {
        super(ANY_CONNECTION_NAME);
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
            return protocol == Protocol.HTTP_PROTOBUF ? url + LOGS_PATH : url;
        }
    }

}
