package io.arconia.dev.services.docling;

import ai.docling.testcontainers.serve.DoclingServeContainer;
import ai.docling.testcontainers.serve.config.DoclingServeContainerConfig;

/**
 * A {@link DoclingServeContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaDoclingServeContainer extends DoclingServeContainer {

    private final DoclingDevServicesProperties properties;

    /**
     * HTTP REST API port.
     */
    protected static final int DOCLING_PORT = 5001;

    public ArconiaDoclingServeContainer(DoclingServeContainerConfig config, DoclingDevServicesProperties properties) {
        super(config);
        this.properties = properties;
    }

    @Override
    protected void configure() {
        super.configure();
        if (properties.getPort() > 0) {
            addFixedExposedPort(properties.getPort(), DOCLING_PORT);
        }
    }

}
