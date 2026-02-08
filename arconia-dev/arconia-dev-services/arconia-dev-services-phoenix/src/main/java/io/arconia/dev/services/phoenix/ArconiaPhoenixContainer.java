package io.arconia.dev.services.phoenix;

import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.util.ContainerUtils;
import io.arconia.testcontainers.phoenix.PhoenixContainer;

/**
 * A {@link PhoenixContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaPhoenixContainer extends PhoenixContainer {

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
        if (ContainerUtils.isValidPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), HTTP_PORT);
        }
        if (ContainerUtils.isValidPort(properties.getOtlpGrpcPort())) {
            addFixedExposedPort(properties.getOtlpGrpcPort(), GRPC_PORT);
        }
    }

}
