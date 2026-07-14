package io.arconia.dev.services.docling;

import ai.docling.testcontainers.serve.DoclingServeContainer;
import ai.docling.testcontainers.serve.config.DoclingServeContainerConfig;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * A {@link DoclingServeContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaDoclingServeContainer extends DoclingServeContainer {

    private final DoclingDevServicesProperties properties;

    static final String COMPATIBLE_IMAGE_NAME = "ghcr.io/docling-project/docling-serve";

    public ArconiaDoclingServeContainer(DoclingDevServicesProperties properties) {
        super(DoclingServeContainerConfig.builder()
                .image(properties.getImageName())
                .enableUi(BootstrapMode.isDev() && properties.isEnableUi())
                .apiKey(properties.getApiKey())
                .containerEnv(properties.getEnvironment())
                .startupTimeout(properties.getStartupTimeout())
                .build());
        this.properties = properties;

        this.withNetworkAliases(properties.getNetworkAliases().toArray(new String[]{}));
        this.withReuse(BootstrapMode.isDev() && properties.isReuse());
        ContainerConfigurer.resources(this, properties);
        ContainerConfigurer.volumes(this, properties);
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isFixedPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), DEFAULT_DOCLING_PORT);
        }
    }

}
