package io.arconia.dev.services.mysql;

import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * An {@link MySQLContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaMySqlContainer extends MySQLContainer {

    private final MySqlDevServicesProperties properties;

    /**
     * MySQL SQL protocol port.
     */
    protected static final int MYSQL_PORT = 3306;

    public ArconiaMySqlContainer(DockerImageName dockerImageName, MySqlDevServicesProperties properties) {
        super(dockerImageName);
        this.properties = properties;
    }

    @Override
    protected void configure() {
        super.configure();
        if (properties.getPort() > 0) {
            addFixedExposedPort(properties.getPort(), MYSQL_PORT);
        }
    }
}
