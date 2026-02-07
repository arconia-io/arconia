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

    public ArconiaDoclingServeContainer(DoclingDevServicesProperties properties) {
        super(DoclingServeContainerConfig.builder()
                .image(properties.getImageName())
                .enableUi(BootstrapMode.isDev() && properties.isEnableUi())
                .containerEnv(properties.getEnvironment())
                .startupTimeout(properties.getStartupTimeout())
                .build());
        this.properties = properties;

        this.setNetworkAliases(properties.getNetworkAliases());
        this.withReuse(BootstrapMode.isDev() && properties.isShared());
        ContainerConfigurer.resources(this, properties);
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isValidPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), DEFAULT_DOCLING_PORT);
        }
    }

}
