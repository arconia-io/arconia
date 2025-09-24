package io.arconia.openinference.observation.testcontainers;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.testcontainers.containers.GenericContainer;

import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingExporterConfiguration;

/**
 * Factory for creating {@link OtlpTracingConnectionDetails} for LGTM containers.
 */
class OpenInferenceOtlpTracingContainerConnectionDetailsFactory
        extends ContainerConnectionDetailsFactory<GenericContainer<?>, OtlpTracingConnectionDetails> {

    private static final Logger logger = LoggerFactory.getLogger(OpenInferenceOtlpTracingContainerConnectionDetailsFactory.class);

    private static final List<String> CONNECTION_NAMES = List.of("phoenix", "arizephoenix/phoenix");

    private static final int HTTP_PORT = 6006;

    OpenInferenceOtlpTracingContainerConnectionDetailsFactory() {
        super(CONNECTION_NAMES, OtlpTracingExporterConfiguration.class.getName());
    }

    @Override
    protected OtlpTracingConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<GenericContainer<?>> source) {
        return new OpenInferenceOtlpTracingContainerConnectionDetails(source);
    }

    private static final class OpenInferenceOtlpTracingContainerConnectionDetails extends ContainerConnectionDetails<GenericContainer<?>> implements OtlpTracingConnectionDetails {

        private static final AtomicBoolean logged = new AtomicBoolean(false);

        private OpenInferenceOtlpTracingContainerConnectionDetails(ContainerConnectionSource<GenericContainer<?>> source) {
            super(source);
        }

        @Override
        public String getUrl(Protocol protocol) {
            String url = switch (protocol) {
                case HTTP_PROTOBUF -> "http://%s:%d%s".formatted(getContainer().getHost(),
                        getContainer().getMappedPort(HTTP_PORT), TRACES_PATH);
                case GRPC ->
                        "http://%s:%d".formatted(getContainer().getHost(), getContainer().getMappedPort(DEFAULT_GRPC_PORT));
            };
            if (logged.compareAndSet(false, true)) {
                logger.info("Phoenix UI: {}", "http://%s:%d".formatted(getContainer().getHost(), getContainer().getMappedPort(HTTP_PORT)));
            }
            return url;
        }

    }

}
