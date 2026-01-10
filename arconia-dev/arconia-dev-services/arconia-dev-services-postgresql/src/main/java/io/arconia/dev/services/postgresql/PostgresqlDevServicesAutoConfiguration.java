package io.arconia.dev.services.postgresql;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.config.DevServicesBeanRegistrations;

/**
 * Auto-configuration for PostgreSQL Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnBooleanProperty(prefix = "arconia.dev.services.postgresql", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(PostgresqlDevServicesProperties.class)
public final class PostgresqlDevServicesAutoConfiguration {

    private static final String COMPATIBLE_IMAGE_NAME = "postgres";

    @Bean
    @ServiceConnection
    @ConditionalOnMissingBean
    PostgreSQLContainer postgresqlContainer(PostgresqlDevServicesProperties properties) {
        return new ArconiaPostgreSqlContainer(DockerImageName.parse(properties.getImageName())
                .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                .withEnv(properties.getEnvironment())
                .withStartupTimeout(properties.getStartupTimeout())
                .withReuse(properties.getShared().asBoolean())
                .withUsername(properties.getUsername())
                .withPassword(properties.getPassword())
                .withDatabaseName(properties.getDbName())
                .withInitScripts(properties.getInitScriptPaths());
    }

    @Bean
    static BeanFactoryPostProcessor postgresqlContainerPostProcessor() {
        return DevServicesBeanRegistrations.beanFactoryPostProcessor(PostgreSQLContainer.class);
    }

}
