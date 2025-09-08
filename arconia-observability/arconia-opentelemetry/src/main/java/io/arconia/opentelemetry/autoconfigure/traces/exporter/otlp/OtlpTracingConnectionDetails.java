package io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp;

import io.arconia.opentelemetry.autoconfigure.exporter.otlp.OtlpConnectionDetails;

/**
 * Connection details to establish a connection to an OTLP endpoint for tracing.
 */
public interface OtlpTracingConnectionDetails extends OtlpConnectionDetails {

    String TRACES_PATH = "/v1/traces";

    int DEFAULT_GRPC_PORT = 4317;
    int DEFAULT_HTTP_PORT = 4318;

    String DEFAULT_GRPC_ENDPOINT = "http://localhost:" + DEFAULT_GRPC_PORT;
    String DEFAULT_HTTP_PROTOBUF_ENDPOINT = "http://localhost:" + DEFAULT_HTTP_PORT + TRACES_PATH;

}
