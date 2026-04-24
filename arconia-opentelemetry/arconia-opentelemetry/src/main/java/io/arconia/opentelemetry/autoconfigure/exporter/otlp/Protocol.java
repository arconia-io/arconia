package io.arconia.opentelemetry.autoconfigure.exporter.otlp;

/**
 * Transport protocol to use for OTLP requests.
 */
public enum Protocol {

    GRPC,

	HTTP_PROTOBUF

}
