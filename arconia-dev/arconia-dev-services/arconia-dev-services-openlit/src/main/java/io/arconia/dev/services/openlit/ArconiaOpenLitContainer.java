package io.arconia.dev.services.openlit;

import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.util.ContainerUtils;
import io.arconia.testcontainers.openlit.OpenLitContainer;

/**
 * An {@link OpenLitContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaOpenLitContainer extends OpenLitContainer {

    static final String COMPATIBLE_IMAGE_NAME = "ghcr.io/openlit/openlit";

    private final OpenLitDevServicesProperties properties;

    ArconiaOpenLitContainer(OpenLitDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()));
        this.properties = properties;
        withClickHouseImage(properties.getClickhouseImageName());
        ContainerConfigurer.base(this, properties);
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isValidPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), UI_PORT);
        }
        if (ContainerUtils.isValidPort(properties.getOtlpGrpcPort())) {
            addFixedExposedPort(properties.getOtlpGrpcPort(), OTLP_GRPC_PORT);
        }
        if (ContainerUtils.isValidPort(properties.getOtlpHttpPort())) {
            addFixedExposedPort(properties.getOtlpHttpPort(), OTLP_HTTP_PORT);
        }
    }

}
