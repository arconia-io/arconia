package io.arconia.docling.autoconfigure;

import java.net.URI;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

/**
 * Connection details to establish a connection to Docling Serve.
 */
public interface DoclingServeConnectionDetails extends ConnectionDetails {

    int DEFAULT_PORT = 5001;

    URI getBaseUrl();

}
