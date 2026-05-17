package io.arconia.opentelemetry.testcontainers;

import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;

import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.otlp.OtlpLoggingConnectionDetails;
import io.arconia.testcontainers.openlit.OpenLitContainer;

/**
 * Factory for creating {@link OtlpLoggingConnectionDetails} for OpenLit containers.
 */
class OpenLitOtlpLoggingContainerConnectionDetailsFactory
        extends ContainerConnectionDetailsFactory<OpenLitContainer, OtlpLoggingConnectionDetails> {

    OpenLitOtlpLoggingContainerConnectionDetailsFactory() {
        super(ANY_CONNECTION_NAME);
    }

    @Override
    protected OtlpLoggingConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<OpenLitContainer> source) {
        return new OpenLitOtlpLoggingContainerConnectionDetails(source);
    }

    private static final class OpenLitOtlpLoggingContainerConnectionDetails
            extends ContainerConnectionDetails<OpenLitContainer> implements OtlpLoggingConnectionDetails {

        private OpenLitOtlpLoggingContainerConnectionDetails(ContainerConnectionSource<OpenLitContainer> source) {
            super(source);
        }

        @Override
        public String getUrl(Protocol protocol) {
            return switch (protocol) {
                case HTTP_PROTOBUF ->
                        "http://%s:%d%s".formatted(getContainer().getHost(), getContainer().getOtlpHttpPort(), LOGS_PATH);
                case GRPC ->
                        "http://%s:%d".formatted(getContainer().getHost(), getContainer().getOtlpGrpcPort());
            };
        }

    }

}
