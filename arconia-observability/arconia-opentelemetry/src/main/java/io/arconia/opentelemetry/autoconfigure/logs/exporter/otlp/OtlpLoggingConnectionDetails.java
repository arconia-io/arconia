package io.arconia.opentelemetry.autoconfigure.logs.exporter.otlp;

import io.arconia.opentelemetry.autoconfigure.exporter.otlp.OtlpConnectionDetails;

/**
 * Connection details to establish a connection to an OTLP endpoint for logging.
 */
public interface OtlpLoggingConnectionDetails extends OtlpConnectionDetails {

    String LOGS_PATH = "/v1/logs";

    String DEFAULT_GRPC_ENDPOINT = "http://localhost:4317";
    String DEFAULT_HTTP_PROTOBUF_ENDPOINT = "http://localhost:4318" + LOGS_PATH;

}
