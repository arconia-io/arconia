package io.arconia.dev.services.docling;

import java.util.ArrayList;
import java.util.List;

import ai.docling.testcontainers.serve.DoclingServeContainer;
import ai.docling.testcontainers.serve.config.DoclingServeContainerConfig;
import com.github.dockerjava.api.command.InspectContainerResponse;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.dev.services.api.registration.DevServiceLinkDefinition;
import io.arconia.dev.services.api.registration.DevServiceLinkProvider;
import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * A {@link DoclingServeContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaDoclingServeContainer extends DoclingServeContainer implements DevServiceLinkProvider {

    private final DoclingDevServicesProperties properties;

    private final boolean uiEnabled;

    static final String COMPATIBLE_IMAGE_NAME = "ghcr.io/docling-project/docling-serve";

    /**
     * The Docling UI is only served in dev mode, and only when the user asks for it.
     */
    private static boolean isUiEnabled(DoclingDevServicesProperties properties) {
        return BootstrapMode.isDev() && properties.isEnableUi();
    }

    public ArconiaDoclingServeContainer(DoclingDevServicesProperties properties) {
        super(DoclingServeContainerConfig.builder()
                .image(properties.getImageName())
                .enableUi(isUiEnabled(properties))
                .apiKey(properties.getApiKey())
                .containerEnv(properties.getEnvironment())
                .startupTimeout(properties.getStartupTimeout())
                .build());
        this.properties = properties;
        this.uiEnabled = isUiEnabled(properties);

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
    public List<DevServiceLinkDefinition> devServiceLinkDefinitions() {
        List<DevServiceLinkDefinition> links = new ArrayList<>();
        if (uiEnabled) {
            links.add(DevServiceLinkDefinition.builder()
                    .id("docling").label("Docling UI").port(DEFAULT_DOCLING_PORT).path("/ui").build());
        }
        links.add(DevServiceLinkDefinition.builder()
                .id("docling-api").label("Docling OpenAPI").port(DEFAULT_DOCLING_PORT).path("/docs").build());
        return List.copyOf(links);
    }

}
