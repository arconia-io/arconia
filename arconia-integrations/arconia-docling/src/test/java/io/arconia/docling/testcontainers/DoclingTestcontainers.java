package io.arconia.docling.testcontainers;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.arconia.docling.Images;
import io.arconia.docling.autoconfigure.client.DoclingConnectionDetails;

@Testcontainers(disabledWithoutDocker = true, parallel = true)
public class DoclingTestcontainers {

    @Container
    @ServiceConnection("docling")
    public static final GenericContainer<?> doclingContainer = new GenericContainer<>(Images.DOCLING)
            .withExposedPorts(DoclingConnectionDetails.DEFAULT_PORT);

}
