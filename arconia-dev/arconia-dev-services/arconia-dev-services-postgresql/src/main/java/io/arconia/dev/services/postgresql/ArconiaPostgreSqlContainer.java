package io.arconia.dev.services.postgresql;

import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * A {@link PostgreSQLContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaPostgreSqlContainer extends PostgreSQLContainer {

    private static final String COMPATIBLE_IMAGE_NAME = "postgres";

    private final PostgresqlDevServicesProperties properties;

    public ArconiaPostgreSqlContainer(PostgresqlDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        this.properties = properties;

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
