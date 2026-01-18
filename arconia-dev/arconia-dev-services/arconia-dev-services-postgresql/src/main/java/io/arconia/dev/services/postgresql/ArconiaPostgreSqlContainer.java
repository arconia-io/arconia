package io.arconia.dev.services.postgresql;

import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A {@link PostgreSQLContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaPostgreSqlContainer extends PostgreSQLContainer {

    private final PostgresqlDevServicesProperties properties;

    /**
     * PostgreSQL SQL protocol port.
     */
    protected static final int POSTGRESQL_PORT = 5432;

    public ArconiaPostgreSqlContainer(DockerImageName dockerImageName, PostgresqlDevServicesProperties properties) {
        super(dockerImageName);
        this.properties = properties;
    }

    @Override
    protected void configure() {
        super.configure();
        if (properties.getPort() > 0) {
            addFixedExposedPort(properties.getPort(), POSTGRESQL_PORT);
        }
    }
}
