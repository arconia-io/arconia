package io.arconia.dev.services.redis;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.redis.RedisDevServicesAutoConfiguration.ConfigurationWithRestart;
import io.arconia.dev.services.redis.RedisDevServicesAutoConfiguration.ConfigurationWithoutRestart;

/**
 * Auto-configuration for Redis Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnProperty(prefix = "arconia.dev.services.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({ConfigurationWithRestart.class, ConfigurationWithoutRestart.class})
@EnableConfigurationProperties(RedisDevServicesProperties.class)
public final class RedisDevServicesAutoConfiguration {

    public static final String COMPATIBLE_IMAGE_NAME = "redis";
    public static final int REDIS_PORT = 6379;

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RestartScope.class)
    public static final class ConfigurationWithRestart {

        @Bean
        @RestartScope
        @ServiceConnection("redis")
        GenericContainer<?> redisContainer(RedisDevServicesProperties properties) {
            return new GenericContainer<>(DockerImageName.parse(properties.getImageName())
                    .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                    .withExposedPorts(REDIS_PORT)
                    .withEnv(properties.getEnvironment())
                    .withStartupTimeout(properties.getStartupTimeout())
                    .withReuse(properties.getShared().asBoolean());
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.springframework.boot.devtools.restart.RestartScope")
    public static final class ConfigurationWithoutRestart {

        @Bean
        @ServiceConnection("redis")
        GenericContainer<?> redisContainerNoRestartScope(RedisDevServicesProperties properties) {
            return new GenericContainer<>(DockerImageName.parse(properties.getImageName())
                    .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                    .withExposedPorts(REDIS_PORT)
                    .withEnv(properties.getEnvironment())
                    .withStartupTimeout(properties.getStartupTimeout())
                    .withReuse(properties.getShared().asBoolean());
        }

    }

}
