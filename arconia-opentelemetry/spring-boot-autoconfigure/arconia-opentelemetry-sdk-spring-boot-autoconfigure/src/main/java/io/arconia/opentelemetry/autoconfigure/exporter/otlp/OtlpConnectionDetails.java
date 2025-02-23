package io.arconia.opentelemetry.autoconfigure.exporter.otlp;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

/**
 * Connection details to establish a connection to an OTLP endpoint.
 */
public interface OtlpConnectionDetails extends ConnectionDetails {

    String getUrl(Protocol protocol);

}
