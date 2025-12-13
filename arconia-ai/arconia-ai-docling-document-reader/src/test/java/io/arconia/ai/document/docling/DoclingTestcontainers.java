package io.arconia.ai.document.docling;

import ai.docling.testcontainers.serve.DoclingServeContainer;
import ai.docling.testcontainers.serve.config.DoclingServeContainerConfig;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true, parallel = true)
class DoclingTestcontainers {

    @Container
    @ServiceConnection("docling")
    static final DoclingServeContainer doclingContainer = new DoclingServeContainer(DoclingServeContainerConfig.builder()
            .image(Images.DOCLING)
            .build());

}
