package io.arconia.dev.services.connections;

import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.testcontainers.grafana.LgtmStackContainer;

import io.arconia.core.support.Internal;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.otlp.OtlpLoggingConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.otlp.OtlpLoggingExporterConfiguration;

/**
 * Factory for creating {@link OtlpLoggingConnectionDetails} for LGTM containers.
 */
@Internal
public class LgtmOtlpLoggingContainerConnectionDetailsFactory
        extends ContainerConnectionDetailsFactory<LgtmStackContainer, OtlpLoggingConnectionDetails> {

    LgtmOtlpLoggingContainerConnectionDetailsFactory() {
        super(ANY_CONNECTION_NAME, OtlpLoggingExporterConfiguration.class.getName());
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
