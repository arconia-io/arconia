package io.arconia.dev.services.docling;

import ai.docling.testcontainers.serve.DoclingServeContainer;
import ai.docling.testcontainers.serve.config.DoclingServeContainerConfig;

import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * A {@link DoclingServeContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaDoclingServeContainer extends DoclingServeContainer {

    private final DoclingDevServicesProperties properties;

    public ArconiaDoclingServeContainer(DoclingServeContainerConfig config, DoclingDevServicesProperties properties) {
        super(config);
        this.properties = properties;
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isValidPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), DEFAULT_DOCLING_PORT);
        }
    }

}
