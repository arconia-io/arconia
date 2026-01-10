package io.arconia.dev.services.docling;

import ai.docling.testcontainers.serve.DoclingServeContainer;
import ai.docling.testcontainers.serve.config.DoclingServeContainerConfig;

/**
 * A {@link DoclingServeContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaDoclingServeContainer extends DoclingServeContainer {

    public ArconiaDoclingServeContainer(DoclingServeContainerConfig config) {
        super(config);
    }

}
