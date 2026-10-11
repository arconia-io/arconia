package io.arconia.dev.services.docling;

import java.util.List;

import ai.docling.testcontainers.serve.DoclingServeContainer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.testcontainers.containers.GenericContainer;

import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationTests;
import io.arconia.dev.services.tests.DisabledIfDockerUnavailable;
import io.arconia.docling.autoconfigure.DoclingServeConnectionDetails;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DoclingDevServicesAutoConfiguration}.
 */
class DoclingDevServicesAutoConfigurationTests extends BaseDevServicesAutoConfigurationTests {

    DoclingDevServicesAutoConfigurationTests() {
        super(DoclingDevServicesAutoConfiguration.class, "docling", DoclingServeContainer.class, DoclingServeConnectionDetails.class);
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        var properties = new DoclingDevServicesProperties();
        return asDiscoverableContainer(new ArconiaDoclingServeContainer(properties), properties, ownerId);
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> discoveredContainer) {
        DoclingServeContainer container = (DoclingServeContainer) discoveredContainer;
        DoclingServeConnectionDetails connectionDetails = context.getBean(DoclingServeConnectionDetails.class);
        assertThat(connectionDetails.getBaseUrl()).hasToString(container.getApiUrl());
    }

    @Test
    @DisabledIfDockerUnavailable
    void devServiceLinksExposeDoclingUiAndApiUrls() {
        contextRunnerWithContainerLifecycle()
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    List<DevServiceLink> links = context.getBean(DevServiceRegistration.class).links();
                    assertThat(links).extracting(DevServiceLink::label).containsExactly("Docling UI", "Docling OpenAPI");
                    assertThat(links).allSatisfy(link -> assertThat(link.url()).startsWith("http://" + container.getHost() + ":"));
                    assertThat(links).filteredOn(link -> link.label().equals("Docling OpenAPI"))
                            .singleElement().satisfies(link -> assertThat(link.url()).endsWith("/docs"));
                });
    }

}
