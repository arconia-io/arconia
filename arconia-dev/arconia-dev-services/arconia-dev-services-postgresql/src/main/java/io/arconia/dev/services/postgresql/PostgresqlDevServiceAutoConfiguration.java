package io.arconia.dev.services.postgresql;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Auto-configuration for PostgreSQL Dev Service.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnProperty(prefix = PostgresqlDevServiceProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PostgresqlDevServiceProperties.class)
public class PostgresqlDevServiceAutoConfiguration {

    public static final String COMPATIBLE_IMAGE_NAME = "postgres";

    @Bean
    @RestartScope
    @ServiceConnection
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "spring.devtools.restart", name = "enabled", havingValue = "true", matchIfMissing = true)
    PostgreSQLContainer<?> postgresqlContainer(PostgresqlDevServiceProperties properties) {
        return new PostgreSQLContainer<>(DockerImageName.parse(properties.getImageName())
                .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                .withReuse(properties.isReusable());
    }

    @Bean
    @ServiceConnection
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "spring.devtools.restart", name = "enabled", havingValue = "false")
    PostgreSQLContainer<?> postgresqlContainerNoRestartScope(PostgresqlDevServiceProperties properties) {
        return new PostgreSQLContainer<>(DockerImageName.parse(properties.getImageName())
                .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                .withReuse(properties.isReusable());
    }


}
