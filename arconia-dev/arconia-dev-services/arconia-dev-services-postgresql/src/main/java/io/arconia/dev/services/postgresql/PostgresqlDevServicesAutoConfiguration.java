package io.arconia.dev.services.postgresql;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.registration.DevServiceCategories;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.core.registration.DiscoveredContainer;
import io.arconia.dev.services.core.registration.JdbcDiscoveredConnectionDetails;
import io.arconia.dev.services.postgresql.PostgresqlDevServicesAutoConfiguration.PostgresqlDevServicesRegistrar;

/**
 * Auto-configuration for PostgreSQL Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("postgresql")
@EnableConfigurationProperties(PostgresqlDevServicesProperties.class)
@Import(PostgresqlDevServicesRegistrar.class)
public final class PostgresqlDevServicesAutoConfiguration {

    static final String COMPATIBLE_IMAGE_NAME = "postgres";

    static final String READY_REGEX = ".*database system is ready to accept connections.*\\s";

    static final String SKIPPING_INITIALIZATION_REGEX = ".*PostgreSQL Database directory appears to contain a database; Skipping initialization:*\\s";

    static PostgreSQLContainer createContainer(PostgresqlDevServicesProperties properties) {
        var container = new PostgreSQLContainer(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        // See: https://github.com/testcontainers/testcontainers-java/issues/4799
        container.waitingFor(Wait.forLogMessage("(" + READY_REGEX + "|" + SKIPPING_INITIALIZATION_REGEX + ")", 2));
        return container;
    }

    static class PostgresqlDevServicesRegistrar extends DevServicesRegistrar {

        private static final String PGVECTOR_STORE_CLASS = "org.springframework.ai.vectorstore.pgvector.PgVectorStore";

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            if (ClassUtils.isPresent(PGVECTOR_STORE_CLASS, null)) {
                setDefaultProperty(PostgresqlDevServicesProperties.CONFIG_PREFIX + ".image-name", "pgvector/pgvector:pg18");
            }

            var properties = bindProperties(PostgresqlDevServicesProperties.CONFIG_PREFIX, PostgresqlDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("postgresql")
                    .description("PostgreSQL Dev Service")
                    .category(DevServiceCategories.JDBC)
                    .properties(properties)
                    .container(PostgreSQLContainer.class, () -> createContainer(properties))
                    .port(PostgreSQLContainer.POSTGRESQL_PORT)
                    .discovery(JdbcConnectionDetails.class,
                            container -> discoveredConnectionDetails(container, properties)));
        }

        static JdbcDiscoveredConnectionDetails discoveredConnectionDetails(DiscoveredContainer container, PostgresqlDevServicesProperties properties) {
            String jdbcUrl = "jdbc:postgresql://%s:%d/%s?loggerLevel=OFF".formatted(container.host(),
                    container.mappedPort(PostgreSQLContainer.POSTGRESQL_PORT), properties.getDbName());
            return new JdbcDiscoveredConnectionDetails(jdbcUrl, properties.getUsername(), properties.getPassword());
        }

    }

}
