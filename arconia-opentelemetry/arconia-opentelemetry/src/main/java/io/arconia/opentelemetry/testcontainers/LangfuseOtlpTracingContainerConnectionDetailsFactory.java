package io.arconia.opentelemetry.testcontainers;

import java.util.Map;

import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;

import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingConnectionDetails;
import io.arconia.testcontainers.langfuse.LangfuseContainer;

/**
 * Factory for creating {@link OtlpTracingConnectionDetails} for Langfuse containers.
 */
class LangfuseOtlpTracingContainerConnectionDetailsFactory
        extends ContainerConnectionDetailsFactory<LangfuseContainer, OtlpTracingConnectionDetails> {

    LangfuseOtlpTracingContainerConnectionDetailsFactory() {
        super(ANY_CONNECTION_NAME);
    }

    @Override
    protected OtlpTracingConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<LangfuseContainer> source) {
        return new LangfuseOtlpTracingContainerConnectionDetails(source);
    }

    private static final class LangfuseOtlpTracingContainerConnectionDetails extends ContainerConnectionDetails<LangfuseContainer> implements OtlpTracingConnectionDetails {

        private LangfuseOtlpTracingContainerConnectionDetails(ContainerConnectionSource<LangfuseContainer> source) {
            super(source);
        }

        @Override
        public String getTracesUrl(Protocol protocol) {
            return switch (protocol) {
                case HTTP_PROTOBUF -> getContainer().getOtlpTracesUrl();
                case GRPC -> throw new IllegalStateException(
                        "Langfuse doesn't support OTLP over gRPC. Use the HTTP/Protobuf protocol instead.");
            };
        }

        /**
         * Langfuse authenticates OTLP requests with the API keys of the project, and requires the
         * ingestion version header from OpenTelemetry exporters.
         */
        @Override
        public Map<String, String> getHeaders() {
            return Map.of(
                    "Authorization", getContainer().getBasicAuthHeader(),
                    "x-langfuse-ingestion-version", "4");
        }

    }

}
