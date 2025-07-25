package io.arconia.dev.services.connections;

import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.testcontainers.grafana.LgtmStackContainer;

import io.arconia.core.support.Internal;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingExporterConfiguration;

/**
 * Factory for creating {@link OtlpTracingConnectionDetails} for LGTM containers.
 */
@Internal
public class LgtmOtlpTracingContainerConnectionDetailsFactory
        extends ContainerConnectionDetailsFactory<LgtmStackContainer, OtlpTracingConnectionDetails> {

    LgtmOtlpTracingContainerConnectionDetailsFactory() {
        super(ANY_CONNECTION_NAME, OtlpTracingExporterConfiguration.class.getName());
    }

    @Override
    protected OtlpTracingConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<LgtmStackContainer> source) {
        return new LgtmOtlpTracingContainerConnectionDetails(source);
    }

    private static final class LgtmOtlpTracingContainerConnectionDetails extends ContainerConnectionDetails<LgtmStackContainer> implements OtlpTracingConnectionDetails {
        private LgtmOtlpTracingContainerConnectionDetails(ContainerConnectionSource<LgtmStackContainer> source) {
            super(source);
        }

        @Override
        public String getUrl(Protocol protocol) {
            String url = switch (protocol) {
                case HTTP_PROTOBUF -> getContainer().getOtlpHttpUrl();
                case GRPC -> getContainer().getOtlpGrpcUrl();
            };
            return protocol == Protocol.HTTP_PROTOBUF ? url + TRACES_PATH : url;
        }
    }

}
