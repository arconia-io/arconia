package io.arconia.opentelemetry.testcontainers;

import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;

import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingConnectionDetails;
import io.arconia.testcontainers.phoenix.PhoenixContainer;

/**
 * Factory for creating {@link OtlpTracingConnectionDetails} for LGTM containers.
 */
class PhoenixOtlpTracingContainerConnectionDetailsFactory
        extends ContainerConnectionDetailsFactory<PhoenixContainer, OtlpTracingConnectionDetails> {

    PhoenixOtlpTracingContainerConnectionDetailsFactory() {
        super(ANY_CONNECTION_NAME);
    }

    @Override
    protected OtlpTracingConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<PhoenixContainer> source) {
        return new PhoenixOtlpTracingContainerConnectionDetails(source);
    }

    private static final class PhoenixOtlpTracingContainerConnectionDetails extends ContainerConnectionDetails<PhoenixContainer> implements OtlpTracingConnectionDetails {

        private PhoenixOtlpTracingContainerConnectionDetails(ContainerConnectionSource<PhoenixContainer> source) {
            super(source);
        }

        @Override
        public String getUrl(Protocol protocol) {
            return switch (protocol) {
                case HTTP_PROTOBUF ->
                        "http://%s:%d%s".formatted(getContainer().getHost(), getContainer().getHttpPort(), TRACES_PATH);
                case GRPC ->
                        "http://%s:%d".formatted(getContainer().getHost(), getContainer().getGrpcPort());
            };
        }

    }

}
