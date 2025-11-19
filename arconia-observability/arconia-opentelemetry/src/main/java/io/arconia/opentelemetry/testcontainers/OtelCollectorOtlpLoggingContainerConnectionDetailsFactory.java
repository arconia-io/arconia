package io.arconia.opentelemetry.testcontainers;

import java.util.List;

import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.testcontainers.containers.Container;

import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.otlp.OtlpLoggingConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.otlp.OtlpLoggingExporterConfiguration;

/**
 * Factory for creating {@link OtlpLoggingConnectionDetails} for OpenTelemetry Collector containers.
 */
class OtelCollectorOtlpLoggingContainerConnectionDetailsFactory
        extends ContainerConnectionDetailsFactory<Container<?>, OtlpLoggingConnectionDetails> {

    private static final List<String> CONNECTION_NAMES = List.of("otel/opentelemetry-collector", "otel/opentelemetry-collector-contrib");

    OtelCollectorOtlpLoggingContainerConnectionDetailsFactory() {
        super(CONNECTION_NAMES, OtlpLoggingExporterConfiguration.class.getName());
    }

    @Override
    protected OtlpLoggingConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<Container<?>> source) {
        return new OtelCollectorOtlpLoggingContainerConnectionDetails(source);
    }

    private static final class OtelCollectorOtlpLoggingContainerConnectionDetails extends ContainerConnectionDetails<Container<?>> implements OtlpLoggingConnectionDetails {

        private OtelCollectorOtlpLoggingContainerConnectionDetails(ContainerConnectionSource<Container<?>> source) {
            super(source);
        }

        @Override
        public String getUrl(Protocol protocol) {
            return switch (protocol) {
                case HTTP_PROTOBUF -> "http://%s:%d%s".formatted(getContainer().getHost(),
                        getContainer().getMappedPort(DEFAULT_HTTP_PORT), LOGS_PATH);
                case GRPC ->
                        "http://%s:%d".formatted(getContainer().getHost(), getContainer().getMappedPort(DEFAULT_GRPC_PORT));
            };
        }

    }

}
