package io.arconia.dev.services.postgresql;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

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
            var properties = bindProperties(PostgresqlDevServicesProperties.CONFIG_PREFIX, PostgresqlDevServicesProperties.class);

            if (ClassUtils.isPresent(PGVECTOR_ENABLED, null) && !StringUtils.hasText(properties.getImageName())) {
                properties.setImageName("pgvector/pgvector:pg18");
            } else if (!StringUtils.hasText(properties.getImageName())) {
                properties.setImageName("postgres:18.1-alpine");
            }

            registry.registerDevService(service -> service
                    .name("postgresql")
                    .description("PostgreSQL Dev Service")
                    .container(container -> container
                            .type(ArconiaPostgreSqlContainer.class)
                            .supplier(() -> new ArconiaPostgreSqlContainer(properties)
                                    .withEnv(properties.getEnvironment())
                                    .withNetworkAliases(properties.getNetworkAliases().toArray(new String[]{}))
                                    .withStartupTimeout(properties.getStartupTimeout())
                                    .withReuse(isDevMode() && properties.isShared())
                                    .withUsername(properties.getUsername())
                                    .withPassword(properties.getPassword())
                                    .withDatabaseName(properties.getDbName())
                                    .withInitScripts(properties.getInitScriptPaths()))
                    )
            );
        }

    }

}
