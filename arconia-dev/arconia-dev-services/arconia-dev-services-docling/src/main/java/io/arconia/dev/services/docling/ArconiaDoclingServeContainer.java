package io.arconia.dev.services.docling;

import java.util.ArrayList;
import java.util.List;

import ai.docling.testcontainers.serve.DoclingServeContainer;
import ai.docling.testcontainers.serve.config.DoclingServeContainerConfig;
import com.github.dockerjava.api.command.InspectContainerResponse;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceLinkProvider;
import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * A {@link DoclingServeContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaDoclingServeContainer extends DoclingServeContainer implements DevServiceLinkProvider {

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
        ContainerConfigurer.resources(this, properties);
        ContainerConfigurer.volumes(this, properties);
        ContainerConfigurer.reuse(this, properties);
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isFixedPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), DEFAULT_DOCLING_PORT);
        }
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        // Suppress the superclass's ad-hoc "Docling Serve UI" log line;
        // Arconia emits a consistent startup message instead.
    }

    @Override
    public List<DevServiceLink> devServiceLinks() {
        List<DevServiceLink> links = new ArrayList<>();
        getUiUrl().ifPresent(url -> links.add(DevServiceLink.builder()
                .id("docling").label("Docling UI").url(url).build()));
        links.add(DevServiceLink.builder()
                .id("docling-api").label("Docling OpenAPI").url("%s/docs".formatted(getApiUrl())).build());
        return List.copyOf(links);
    }

}
