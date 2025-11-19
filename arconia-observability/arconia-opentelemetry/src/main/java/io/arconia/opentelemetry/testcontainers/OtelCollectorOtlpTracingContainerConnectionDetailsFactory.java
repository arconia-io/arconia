package io.arconia.opentelemetry.testcontainers;

import java.util.List;

import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.testcontainers.containers.Container;

import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingExporterConfiguration;

/**
 * Factory for creating {@link OtlpTracingConnectionDetails} for OpenTelemetry Collector containers.
 */
class OtelCollectorOtlpTracingContainerConnectionDetailsFactory
        extends ContainerConnectionDetailsFactory<Container<?>, OtlpTracingConnectionDetails> {

    private static final List<String> CONNECTION_NAMES = List.of("otel/opentelemetry-collector", "otel/opentelemetry-collector-contrib");

    OtelCollectorOtlpTracingContainerConnectionDetailsFactory() {
        super(CONNECTION_NAMES, OtlpTracingExporterConfiguration.class.getName());
    }

    @Override
    protected OtlpTracingConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<Container<?>> source) {
        return new OtelCollectorOtlpTracingContainerConnectionDetails(source);
    }

    private static final class OtelCollectorOtlpTracingContainerConnectionDetails extends ContainerConnectionDetails<Container<?>> implements OtlpTracingConnectionDetails {

        private OtelCollectorOtlpTracingContainerConnectionDetails(ContainerConnectionSource<Container<?>> source) {
            super(source);
        }

        @Override
        public String getUrl(Protocol protocol) {
            return switch (protocol) {
                case HTTP_PROTOBUF -> "http://%s:%d%s".formatted(getContainer().getHost(),
                        getContainer().getMappedPort(DEFAULT_HTTP_PORT), TRACES_PATH);
                case GRPC ->
                        "http://%s:%d".formatted(getContainer().getHost(), getContainer().getMappedPort(DEFAULT_GRPC_PORT));
            };
        }

    }

}
