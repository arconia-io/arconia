package io.arconia.dev.services.phoenix;

import java.util.List;

import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.api.registration.DevServiceLinkDefinition;
import io.arconia.dev.services.api.registration.DevServiceLinkProvider;
import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.util.ContainerUtils;
import io.arconia.testcontainers.phoenix.PhoenixContainer;

/**
 * A {@link PhoenixContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaPhoenixContainer extends PhoenixContainer implements DevServiceLinkProvider {

    private final PhoenixDevServicesProperties properties;

    static final String COMPATIBLE_IMAGE_NAME = "arizephoenix/phoenix";

    public ArconiaPhoenixContainer(PhoenixDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        this.properties = properties;

        ContainerConfigurer.base(this, properties);
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isFixedPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), HTTP_PORT);
        }
        if (ContainerUtils.isFixedPort(properties.getOtlpGrpcPort())) {
            addFixedExposedPort(properties.getOtlpGrpcPort(), GRPC_PORT);
        }
    }

    @Override
    public List<DevServiceLinkDefinition> devServiceLinkDefinitions() {
        return List.of(DevServiceLinkDefinition.builder()
                .id("phoenix")
                .label("Phoenix UI")
                .port(HTTP_PORT)
                .build());
    }

}
