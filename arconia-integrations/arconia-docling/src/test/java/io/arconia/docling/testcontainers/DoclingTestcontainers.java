package io.arconia.docling.testcontainers;

import ai.docling.testcontainers.serve.DoclingServeContainer;
import ai.docling.testcontainers.serve.config.DoclingServeContainerConfig;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.arconia.docling.Images;

@Testcontainers(disabledWithoutDocker = true, parallel = true)
public class DoclingTestcontainers {

    public static final String API_KEY = "test-api-key";

    @Container
    @ServiceConnection("docling")
    public static final DoclingServeContainer doclingContainer = new DoclingServeContainer(DoclingServeContainerConfig.builder()
            .image(Images.DOCLING)
            .apiKey(API_KEY)
            .build());

}
