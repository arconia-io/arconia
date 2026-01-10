package io.arconia.dev.services.mysql;

import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * An {@link MySQLContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaMySqlContainer extends MySQLContainer {

    public ArconiaMySqlContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
    }

}
