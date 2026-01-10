package io.arconia.dev.services.postgresql;

import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A {@link PostgreSQLContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaPostgreSqlContainer extends PostgreSQLContainer {

    public ArconiaPostgreSqlContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
    }

}
