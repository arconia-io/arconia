package io.arconia.dev.services.mariadb;

import org.testcontainers.mariadb.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A {@link MariaDBContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaMariaDbContainer extends MariaDBContainer {

    private final MariaDbDevServicesProperties properties;

    /**
     * MariaDB/MySQL SQL protocol port.
     */
    protected static final int MARIADB_PORT = 3306;

    public ArconiaMariaDbContainer(DockerImageName dockerImageName, MariaDbDevServicesProperties properties) {
        super(dockerImageName);
        this.properties = properties;
    }

    @Override
    protected void configure() {
        super.configure();
        if (properties.getPort() > 0) {
            addFixedExposedPort(properties.getPort(), MARIADB_PORT);
        }
    }
}
