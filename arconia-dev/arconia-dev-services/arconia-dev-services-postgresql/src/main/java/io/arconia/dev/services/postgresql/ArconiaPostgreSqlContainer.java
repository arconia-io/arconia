package io.arconia.dev.services.postgresql;

import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * A {@link PostgreSQLContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaPostgreSqlContainer extends PostgreSQLContainer {

    private final PostgresqlDevServicesProperties properties;

    static final String COMPATIBLE_IMAGE_NAME = "postgres";

    static final String READY_REGEX = ".*database system is ready to accept connections.*\\s";
    static final String SKIPPING_INITIALIZATION_REGEX = ".*PostgreSQL Database directory appears to contain a database; Skipping initialization:*\\s";

    public ArconiaPostgreSqlContainer(PostgresqlDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        this.properties = properties;

        // See: https://github.com/testcontainers/testcontainers-java/issues/4799
        this.waitingFor(Wait
                .forLogMessage("(" + READY_REGEX + ")?(" + SKIPPING_INITIALIZATION_REGEX + ")?", 2)
        );

        ContainerConfigurer.base(this, properties);
        ContainerConfigurer.jdbc(this, properties);
    }

    @Override
    protected void configure() {
        super.configure();

        if (ContainerUtils.isValidPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), POSTGRESQL_PORT);
        }
    }

}
