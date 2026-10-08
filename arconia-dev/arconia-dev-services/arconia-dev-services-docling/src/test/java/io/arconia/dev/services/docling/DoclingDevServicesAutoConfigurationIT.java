package io.arconia.dev.services.docling;

import java.util.List;

import ai.docling.testcontainers.serve.DoclingServeContainer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationIT;
import io.arconia.docling.autoconfigure.DoclingServeConnectionDetails;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link DoclingDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class DoclingDevServicesAutoConfigurationIT extends BaseDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = defaultContextRunner(DoclingDevServicesAutoConfiguration.class);

    @Override
    protected ApplicationContextRunner getContextRunner() {
        return contextRunner;
    }

    @Override
    protected Class<?> getAutoConfigurationClass() {
        return DoclingDevServicesAutoConfiguration.class;
    }

    @Override
    protected Class<? extends GenericContainer<?>> getContainerClass() {
        return DoclingServeContainer.class;
    }

    @Override
    protected String getServiceName() {
        return "docling";
    }

    @Override
    protected Class<?> getConnectionDetailsClass() {
        return DoclingServeConnectionDetails.class;
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
