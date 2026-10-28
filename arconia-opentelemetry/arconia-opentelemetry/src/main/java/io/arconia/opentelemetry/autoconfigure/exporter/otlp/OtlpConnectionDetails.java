package io.arconia.opentelemetry.autoconfigure.exporter.otlp;

import java.util.Map;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

/**
 * Connection details to establish a connection to an OTLP endpoint.
 * <p>
 * Each signal-specific sub-interface declares the endpoint method for its signal,
 * so a single implementation can provide connection details for multiple signals.
 */
public interface OtlpConnectionDetails extends ConnectionDetails {

    int DEFAULT_GRPC_PORT = 4317;
    int DEFAULT_HTTP_PORT = 4318;

    /**
     * Headers to include in each request to the endpoint, such as the credentials required by the backend.
     * Headers configured via properties take precedence over these.
     */
    default Map<String, String> getHeaders() {
        return Map.of();
    }

}
