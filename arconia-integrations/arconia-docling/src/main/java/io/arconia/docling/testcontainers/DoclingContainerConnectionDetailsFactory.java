package io.arconia.docling.testcontainers;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.testcontainers.containers.GenericContainer;

import io.arconia.docling.autoconfigure.client.DoclingConnectionDetails;

/**
 * Factory for creating {@link DoclingConnectionDetails} for a Docling container.
 */
class DoclingContainerConnectionDetailsFactory
        extends ContainerConnectionDetailsFactory<GenericContainer<?>, DoclingConnectionDetails> {

    private static final Logger logger = LoggerFactory.getLogger(DoclingContainerConnectionDetailsFactory.class);

    private static final List<String> DOCLING_CONNECTION_NAMES = List.of("docling", "ghcr.io/docling-project/docling-serve",
            "quay.io/docling-project/docling-serve");

    DoclingContainerConnectionDetailsFactory() {
        super(DOCLING_CONNECTION_NAMES);
    }

    @Override
    protected DoclingConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<GenericContainer<?>> source) {
        return new DoclingContainerConnectionDetails(source);
    }

    private static final class DoclingContainerConnectionDetails extends ContainerConnectionDetails<GenericContainer<?>> implements DoclingConnectionDetails {
        private DoclingContainerConnectionDetails(ContainerConnectionSource<GenericContainer<?>> source) {
            super(source);
        }

        @Override
        public URI getUrl() {
            URI url = URI.create("http://%s:%d".formatted(getContainer().getHost(), getContainer().getMappedPort(DEFAULT_PORT)));
            logger.info("Docling UI: {}/ui", url);
            return url;
        }
    }

}
