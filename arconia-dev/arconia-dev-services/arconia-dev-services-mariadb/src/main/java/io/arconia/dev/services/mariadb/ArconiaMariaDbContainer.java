package io.arconia.dev.services.mariadb;

import org.testcontainers.mariadb.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * A {@link MariaDBContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaMariaDbContainer extends MariaDBContainer {

    private final MariaDbDevServicesProperties properties;

    static final String COMPATIBLE_IMAGE_NAME = "mariadb";

    static final Integer MARIADB_PORT = 3306;

    public ArconiaMariaDbContainer(MariaDbDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        this.properties = properties;

        ContainerConfigurer.base(this, properties);
        ContainerConfigurer.jdbc(this, properties);
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isValidPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), MARIADB_PORT);
        }
    }

}
