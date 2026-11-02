package io.arconia.dev.services.langfuse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import io.arconia.dev.services.core.registration.DiscoveredContainer;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingConnectionDetails;
import io.arconia.testcontainers.langfuse.LangfuseContainer;

/**
 * OTLP connection details for traces to connect to a Langfuse dev service
 * running in a container discovered from another application.
 * <p>
 * The API keys are the ones configured for this application, which must match the ones
 * of the discovered Langfuse instance.
 */
final class LangfuseDiscoveredConnectionDetails implements OtlpTracingConnectionDetails {

    private final String tracesUrl;

    private final Map<String, String> headers;

    LangfuseDiscoveredConnectionDetails(DiscoveredContainer container, LangfuseDevServicesProperties properties) {
        this.tracesUrl = "http://%s:%d%s".formatted(container.host(), container.mappedPort(LangfuseContainer.HTTP_PORT),
                LangfuseContainer.OTLP_TRACES_PATH);
        String credentials = properties.getPublicKey() + ":" + properties.getSecretKey();
        this.headers = Map.of(
                "Authorization", "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)),
                "x-langfuse-ingestion-version", "4");
    }

    @Override
    public String getTracesUrl(Protocol protocol) {
        return switch (protocol) {
            case HTTP_PROTOBUF -> tracesUrl;
            case GRPC -> throw new IllegalStateException(
                    "Langfuse doesn't support OTLP over gRPC. Use the HTTP/Protobuf protocol instead.");
        };
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

}
