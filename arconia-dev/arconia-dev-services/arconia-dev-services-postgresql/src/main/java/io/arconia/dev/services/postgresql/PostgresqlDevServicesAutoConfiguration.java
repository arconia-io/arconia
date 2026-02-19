package io.arconia.dev.services.postgresql;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.autoconfigure.DevServicesAutoConfiguration;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.postgresql.PostgresqlDevServicesAutoConfiguration.PostgresqlDevServicesRegistrar;

/**
 * Auto-configuration for PostgreSQL Dev Services.
 */
@AutoConfiguration(after = DevServicesAutoConfiguration.class, before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("postgresql")
@EnableConfigurationProperties(PostgresqlDevServicesProperties.class)
@Import(PostgresqlDevServicesRegistrar.class)
public final class PostgresqlDevServicesAutoConfiguration {

    static class PostgresqlDevServicesRegistrar extends DevServicesRegistrar {

        private static final String PGVECTOR_ENABLED = "org.springframework.ai.vectorstore.pgvector.PgVectorStore";

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            String defaultImageName;
            if (ClassUtils.isPresent(PGVECTOR_ENABLED, null)) {
                defaultImageName = "pgvector/pgvector:pg18";
            } else {
                defaultImageName = "postgres:18.2-alpine";
            }
            setDefaultProperty(PostgresqlDevServicesProperties.CONFIG_PREFIX + ".image-name", defaultImageName);

            var properties = bindProperties(PostgresqlDevServicesProperties.CONFIG_PREFIX, PostgresqlDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("postgresql")
                    .description("PostgreSQL Dev Service")
                    .container(container -> container
                            .type(ArconiaPostgreSqlContainer.class)
                            .supplier(() -> new ArconiaPostgreSqlContainer(properties))
                    )
            );
        }

    }

}
