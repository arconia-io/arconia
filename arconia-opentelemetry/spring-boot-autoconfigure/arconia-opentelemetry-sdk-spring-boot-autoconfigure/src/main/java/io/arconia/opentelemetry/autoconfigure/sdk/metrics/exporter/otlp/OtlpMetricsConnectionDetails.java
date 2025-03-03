package io.arconia.opentelemetry.autoconfigure.sdk.metrics.exporter.otlp;

import io.arconia.opentelemetry.autoconfigure.sdk.exporter.otlp.OtlpConnectionDetails;

/**
 * Connection details to establish a connection to an OTLP endpoint for metrics.
 */
public interface OtlpMetricsConnectionDetails extends OtlpConnectionDetails {
  
    static final String METRICS_PATH = "/v1/metrics";

    static final String DEFAULT_GRPC_ENDPOINT = "http://localhost:4317";
    static final String DEFAULT_HTTP_PROTOBUF_ENDPOINT = "http://localhost:4318" + METRICS_PATH;

}
