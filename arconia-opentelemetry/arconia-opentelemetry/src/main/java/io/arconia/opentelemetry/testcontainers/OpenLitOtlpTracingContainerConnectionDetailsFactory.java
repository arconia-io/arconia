package io.arconia.opentelemetry.testcontainers;

import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;

import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingConnectionDetails;
import io.arconia.testcontainers.openlit.OpenLitContainer;

/**
 * Factory for creating {@link OtlpTracingConnectionDetails} for OpenLit containers.
 */
class OpenLitOtlpTracingContainerConnectionDetailsFactory
        extends ContainerConnectionDetailsFactory<OpenLitContainer, OtlpTracingConnectionDetails> {

    OpenLitOtlpTracingContainerConnectionDetailsFactory() {
        super(ANY_CONNECTION_NAME);
    }

    @Override
    protected OtlpTracingConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<OpenLitContainer> source) {
        return new OpenLitOtlpTracingContainerConnectionDetails(source);
    }

    private static final class OpenLitOtlpTracingContainerConnectionDetails
            extends ContainerConnectionDetails<OpenLitContainer> implements OtlpTracingConnectionDetails {

        private OpenLitOtlpTracingContainerConnectionDetails(ContainerConnectionSource<OpenLitContainer> source) {
            super(source);
        }

        @Override
        public String getUrl(Protocol protocol) {
            return switch (protocol) {
                case HTTP_PROTOBUF ->
                        "http://%s:%d%s".formatted(getContainer().getHost(), getContainer().getOtlpHttpPort(), TRACES_PATH);
                case GRPC ->
                        "http://%s:%d".formatted(getContainer().getHost(), getContainer().getOtlpGrpcPort());
            };
        }

    }

}
