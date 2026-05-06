package io.arconia.dev.services.garage;

import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.util.ContainerUtils;
import io.arconia.testcontainers.garage.GarageContainer;

/**
 * A {@link GarageContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaGarageContainer extends GarageContainer {

    private final GarageDevServicesProperties properties;

    static final String COMPATIBLE_IMAGE_NAME = "dxflrs/garage";

    ArconiaGarageContainer(GarageDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        this.properties = properties;

        withDefaultBucket(properties.getBucketName());

        ContainerConfigurer.base(this, properties);
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isValidPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), S3_API_PORT);
        }
    }

}
