package io.arconia.opentelemetry.autoconfigure.exporter.otlp;

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

}
