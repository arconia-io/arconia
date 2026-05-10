package io.arconia.dev.services.floci;

import io.floci.testcontainers.FlociContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * A {@link FlociContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaFlociContainer extends FlociContainer {

    private final FlociDevServicesProperties properties;

    static final String COMPATIBLE_IMAGE_NAME = "floci/floci";

    ArconiaFlociContainer(FlociDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        this.properties = properties;

        ContainerConfigurer.base(this, properties);
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isValidPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), FlociContainer.PORT);
        }
    }

}
