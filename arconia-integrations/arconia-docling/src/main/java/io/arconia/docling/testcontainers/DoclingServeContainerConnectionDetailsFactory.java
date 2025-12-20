package io.arconia.docling.testcontainers;

import java.net.URI;

import ai.docling.testcontainers.serve.DoclingServeContainer;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;

import io.arconia.docling.autoconfigure.DoclingAutoConfiguration;
import io.arconia.docling.autoconfigure.DoclingServeConnectionDetails;

/**
 * Factory for creating {@link DoclingServeConnectionDetails} for a Docling Serve container.
 */
class DoclingServeContainerConnectionDetailsFactory extends ContainerConnectionDetailsFactory<DoclingServeContainer, DoclingServeConnectionDetails> {

    private static final String API_KEY_ENV_VAR = "DOCLING_SERVE_API_KEY";

    DoclingServeContainerConnectionDetailsFactory() {
        super(ANY_CONNECTION_NAME, DoclingAutoConfiguration.class.getName());
    }

    @Override
    protected DoclingServeConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<DoclingServeContainer> source) {
        return new DoclingContainerServeConnectionDetails(source);
    }

    private static final class DoclingContainerServeConnectionDetails extends ContainerConnectionDetails<DoclingServeContainer> implements DoclingServeConnectionDetails {

        private DoclingContainerServeConnectionDetails(ContainerConnectionSource<DoclingServeContainer> source) {
            super(source);
        }

        @Override
        public URI getBaseUrl() {
            return URI.create("http://%s:%d".formatted(getContainer().getHost(), getContainer().getMappedPort(DEFAULT_PORT)));
        }

        @Override
        @Nullable
        public String getApiKey() {
            return getContainer().getEnvMap().get(API_KEY_ENV_VAR);
        }

    }

}
