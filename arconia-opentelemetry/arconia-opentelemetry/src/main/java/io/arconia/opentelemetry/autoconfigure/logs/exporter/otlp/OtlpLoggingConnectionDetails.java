package io.arconia.opentelemetry.autoconfigure.logs.exporter.otlp;

import io.arconia.opentelemetry.autoconfigure.exporter.otlp.OtlpConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;

/**
 * Connection details to establish a connection to an OTLP endpoint for logging.
 */
public interface OtlpLoggingConnectionDetails extends OtlpConnectionDetails {

    String LOGS_PATH = "/v1/logs";

    String DEFAULT_GRPC_ENDPOINT = "http://localhost:" + DEFAULT_GRPC_PORT;
    String DEFAULT_HTTP_PROTOBUF_ENDPOINT = "http://localhost:" + DEFAULT_HTTP_PORT + LOGS_PATH;

    String getLogsUrl(Protocol protocol);

}
