package io.arconia.docling.autoconfigure.client;

import java.net.URI;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

import io.arconia.core.support.Incubating;

/**
 * Connection details to establish a connection to a Docling server.
 */
@Incubating(since = "0.15.0")
public interface DoclingConnectionDetails extends ConnectionDetails {

    int DEFAULT_PORT = 5001;

    URI getUrl();

}
