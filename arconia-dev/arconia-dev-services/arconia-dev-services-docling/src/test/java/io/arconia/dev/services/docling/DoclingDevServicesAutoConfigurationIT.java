package io.arconia.dev.services.docling;

import java.util.List;

import ai.docling.testcontainers.serve.DoclingServeContainer;
import ai.docling.testcontainers.serve.config.DoclingServeContainerConfig;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceLinkProvider;
import io.arconia.dev.services.core.container.DevServiceLabels;
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
    protected boolean supportsSharing() {
        return true;
    }

    @Override
    protected GenericContainer<?> createSharedContainer(String ownerId) {
        DoclingDevServicesProperties properties = new DoclingDevServicesProperties();
        DoclingServeContainer container = new DoclingServeContainer(DoclingServeContainerConfig.builder()
                .image(properties.getImageName())
                .apiKey(properties.getApiKey())
                .build());
        return withSharedLabels(container, ownerId);
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> sharedContainer) {
        DoclingDevServicesProperties properties = new DoclingDevServicesProperties();
        DoclingServeConnectionDetails connectionDetails = context.getBean(DoclingServeConnectionDetails.class);
        assertThat(connectionDetails.getBaseUrl().getPort())
                .isEqualTo(sharedContainer.getMappedPort(DoclingServeConnectionDetails.DEFAULT_PORT));
        assertThat(connectionDetails.getApiKey()).isEqualTo(properties.getApiKey());
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
                    assertThat(container.isShouldBeReused()).isFalse();
                    assertThat(container.getBinds()).isEmpty();
                    assertThat(container.getLabels())
                            .containsEntry(DevServiceLabels.NAME, "docling")
                            .containsEntry(DevServiceLabels.SHARED, "true")
                            .containsEntry(DevServiceLabels.OWNER, DevServiceLabels.ownerId());

                    assertThatHasSingletonScope(context);
                });
    }

    @Test
    void devServiceLinksExposeDoclingUiAndApiUrls() {
        // In dev mode the UI is enabled by default, so both the UI and OpenAPI links are exposed.
        getContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    container.start();
                    List<DevServiceLink> links = ((DevServiceLinkProvider) container).devServiceLinks();
                    assertThat(links).extracting(DevServiceLink::id).containsExactly("docling", "docling-api");
                    assertThat(links).allSatisfy(link -> assertThat(link.url()).startsWith("http://"));
                    assertThat(links).filteredOn(link -> link.id().equals("docling-api"))
                            .singleElement().satisfies(link -> assertThat(link.url()).endsWith("/docs"));
                    container.stop();
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
