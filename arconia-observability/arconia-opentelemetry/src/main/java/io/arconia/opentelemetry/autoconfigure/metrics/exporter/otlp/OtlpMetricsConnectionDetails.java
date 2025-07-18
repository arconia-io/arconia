package io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp;

import io.arconia.opentelemetry.autoconfigure.exporter.otlp.OtlpConnectionDetails;

/**
 * Connection details to establish a connection to an OTLP endpoint for metrics.
 */
public interface OtlpMetricsConnectionDetails extends OtlpConnectionDetails {

    String METRICS_PATH = "/v1/metrics";

    String DEFAULT_GRPC_ENDPOINT = "http://localhost:4317";
    String DEFAULT_HTTP_PROTOBUF_ENDPOINT = "http://localhost:4318" + METRICS_PATH;

}
