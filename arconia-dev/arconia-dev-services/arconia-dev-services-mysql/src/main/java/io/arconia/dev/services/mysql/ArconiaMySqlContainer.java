package io.arconia.dev.services.mysql;

import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * An {@link MySQLContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaMySqlContainer extends MySQLContainer {

    private static final String COMPATIBLE_IMAGE_NAME = "mysql";

    private final MySqlDevServicesProperties properties;

    static final int MYSQL_PORT = 3306;

    public ArconiaMySqlContainer(MySqlDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        this.properties = properties;
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isValidPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), MYSQL_PORT);
        }
    }

}
