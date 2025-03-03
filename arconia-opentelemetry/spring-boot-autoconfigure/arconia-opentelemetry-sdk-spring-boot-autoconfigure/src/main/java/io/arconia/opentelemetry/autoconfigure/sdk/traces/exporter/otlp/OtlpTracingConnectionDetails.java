package io.arconia.opentelemetry.autoconfigure.sdk.traces.exporter.otlp;

import io.arconia.opentelemetry.autoconfigure.sdk.exporter.otlp.OtlpConnectionDetails;

/**
 * Connection details to establish a connection to an OTLP endpoint for tracing.
 */
public interface OtlpTracingConnectionDetails extends OtlpConnectionDetails {

    static final String TRACES_PATH = "/v1/traces";

    static final String DEFAULT_GRPC_ENDPOINT = "http://localhost:4317";
    static final String DEFAULT_HTTP_PROTOBUF_ENDPOINT = "http://localhost:4318" + TRACES_PATH;

}
