package io.arconia.opentelemetry.testcontainers;

import java.util.List;

import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.testcontainers.containers.Container;

import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp.OtlpMetricsConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp.OtlpMetricsExporterConfiguration;

/**
 * Factory for creating {@link OtlpMetricsConnectionDetails} for OpenTelemetry Collector containers.
 */
class OtelCollectorOtlpMetricsContainerConnectionDetailsFactory
        extends ContainerConnectionDetailsFactory<Container<?>, OtlpMetricsConnectionDetails> {

    private static final List<String> CONNECTION_NAMES = List.of("otel/opentelemetry-collector", "otel/opentelemetry-collector-contrib");

    OtelCollectorOtlpMetricsContainerConnectionDetailsFactory() {
        super(CONNECTION_NAMES, OtlpMetricsExporterConfiguration.class.getName());
    }

    @Override
    protected OtlpMetricsConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<Container<?>> source) {
        return new OtelCollectorOtlpMetricsContainerConnectionDetails(source);
    }

    private static final class OtelCollectorOtlpMetricsContainerConnectionDetails extends ContainerConnectionDetails<Container<?>> implements OtlpMetricsConnectionDetails {

        private OtelCollectorOtlpMetricsContainerConnectionDetails(ContainerConnectionSource<Container<?>> source) {
            super(source);
        }

        @Override
        public String getUrl(Protocol protocol) {
            return switch (protocol) {
                case HTTP_PROTOBUF -> "http://%s:%d%s".formatted(getContainer().getHost(),
                        getContainer().getMappedPort(DEFAULT_HTTP_PORT), METRICS_PATH);
                case GRPC ->
                        "http://%s:%d".formatted(getContainer().getHost(), getContainer().getMappedPort(DEFAULT_GRPC_PORT));
            };
        }

    }

}
