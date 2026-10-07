package io.arconia.dev.services.docling;

import java.util.List;

import ai.docling.testcontainers.serve.DoclingServeContainer;

import org.apache.commons.lang3.ArrayUtils;
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
        return withDiscoveryLabels(new ArconiaDoclingServeContainer(new DoclingDevServicesProperties()), ownerId);
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> discoveredContainer) {
        DoclingServeContainer container = (DoclingServeContainer) discoveredContainer;
        DoclingServeConnectionDetails connectionDetails = context.getBean(DoclingServeConnectionDetails.class);
        assertThat(connectionDetails.getBaseUrl()).hasToString(container.getApiUrl());
    }

    @Test
    void containerAvailableInDevMode() {
        getContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> {
                    assertThat(context).hasSingleBean(getContainerClass());
                    var container = context.getBean(getContainerClass());
                    assertThat(container.getDockerImageName()).contains(ArconiaDoclingServeContainer.COMPATIBLE_IMAGE_NAME);
                    assertThat(container.getEnv()).contains("DOCLING_SERVE_ENABLE_UI=true");
                    assertThat(container.getNetworkAliases()).hasSize(1);
                    assertThat(container.getBinds()).isEmpty();

                    assertThatHasSingletonScope(context);
                });
    }

    @Test
    void devServiceLinksExposeDoclingUiAndApiUrls() {
        // In dev mode the UI is enabled by default, so both the UI and OpenAPI links are exposed.
        contextRunnerWithContainerLifecycle()
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    List<DevServiceLink> links = context.getBean(DevServiceRegistration.class).links();
                    assertThat(links).extracting(DevServiceLink::label).containsExactly("Docling UI", "Docling OpenAPI");
                    assertThat(links).allSatisfy(link -> assertThat(link.url()).startsWith("http://" + container.getHost() + ":"));
                    assertThat(links).filteredOn(link -> link.label().equals("Docling OpenAPI"))
                            .singleElement().satisfies(link -> assertThat(link.url()).endsWith("/docs"));
                });
    }

    @Test
    void containerConfigurationApplied() {
        String[] properties = ArrayUtils.addAll(commonConfigurationProperties(),
                "arconia.dev.services.%s.enable-ui=false".formatted(getServiceName())
        );

        getContextRunner()
                .withPropertyValues(properties)
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    container.start();
                    assertThatConfigurationIsApplied(container);
                    container.stop();
                });
    }

}
