package io.arconia.docling.testcontainers;

import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;

import ai.docling.testcontainers.serve.DoclingServeContainer;

import io.arconia.docling.autoconfigure.DoclingServeConnectionDetails;

/**
 * Factory for creating {@link DoclingServeConnectionDetails} for a Docling Serve container.
 */
class DoclingServeContainerConnectionDetailsFactory extends ContainerConnectionDetailsFactory<DoclingServeContainer, DoclingServeConnectionDetails> {

    private static final Logger logger = LoggerFactory.getLogger(DoclingServeContainerConnectionDetailsFactory.class);

    private static final List<String> DOCLING_CONNECTION_NAMES = List.of("docling", "ghcr.io/docling-project/docling-serve",
            "quay.io/docling-project/docling-serve");

    DoclingServeContainerConnectionDetailsFactory() {
        super(DOCLING_CONNECTION_NAMES);
    }

    @Override
    protected DoclingServeConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<DoclingServeContainer> source) {
        return new DoclingContainerServeConnectionDetails(source);
    }

    private static final class DoclingContainerServeConnectionDetails extends ContainerConnectionDetails<DoclingServeContainer> implements DoclingServeConnectionDetails {

        private static final AtomicBoolean logged = new AtomicBoolean(false);

        private DoclingContainerServeConnectionDetails(ContainerConnectionSource<DoclingServeContainer> source) {
            super(source);
        }

        @Override
        public URI getUrl() {
            URI url = URI.create("http://%s:%d".formatted(getContainer().getHost(), getContainer().getMappedPort(DEFAULT_PORT)));

            if (logged.compareAndSet(false, true)) {
                logger.info("Docling Serve UI: {}/ui", url);
            }

            return url;
        }
    }

}
