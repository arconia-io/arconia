package io.arconia.opentelemetry.autoconfigure.sdk.logs.exporter.otlp;

import io.arconia.opentelemetry.autoconfigure.sdk.exporter.otlp.OtlpConnectionDetails;

/**
 * Connection details to establish a connection to an OTLP endpoint for logging.
 */
public interface OtlpLoggingConnectionDetails extends OtlpConnectionDetails {

    static final String LOGS_PATH = "/v1/logs";

    static final String DEFAULT_GRPC_ENDPOINT = "http://localhost:4317";
    static final String DEFAULT_HTTP_PROTOBUF_ENDPOINT = "http://localhost:4318" + LOGS_PATH;

}
