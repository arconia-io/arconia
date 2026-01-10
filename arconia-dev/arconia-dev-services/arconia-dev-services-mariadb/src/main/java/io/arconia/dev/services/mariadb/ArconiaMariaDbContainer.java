package io.arconia.dev.services.mariadb;

import org.testcontainers.mariadb.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A {@link MariaDBContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaMariaDbContainer extends MariaDBContainer {

    public ArconiaMariaDbContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
    }

}
