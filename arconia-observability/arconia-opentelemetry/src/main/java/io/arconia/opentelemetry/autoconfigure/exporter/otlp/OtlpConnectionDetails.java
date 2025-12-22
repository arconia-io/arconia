package io.arconia.opentelemetry.autoconfigure.exporter.otlp;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

/**
 * Connection details to establish a connection to an OTLP endpoint.
 */
public interface OtlpConnectionDetails extends ConnectionDetails {

    int DEFAULT_GRPC_PORT = 4317;
    int DEFAULT_HTTP_PORT = 4318;

    String getUrl(Protocol protocol);

}
