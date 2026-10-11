package io.arconia.dev.services.redis;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.autoconfigure.DevServicesAutoConfiguration;
import io.arconia.dev.services.core.registration.DevServiceCategories;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.redis.RedisDevServicesAutoConfiguration.RedisDevServicesRegistrar;
import io.arconia.testcontainers.redis.RedisContainer;

/**
 * Auto-configuration for Redis Dev Services.
 */
@AutoConfiguration(after = DevServicesAutoConfiguration.class, before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("redis")
@EnableConfigurationProperties(RedisDevServicesProperties.class)
@Import(RedisDevServicesRegistrar.class)
public final class RedisDevServicesAutoConfiguration {

    static final String COMPATIBLE_IMAGE_NAME = "redis";

    static RedisContainer createContainer(RedisDevServicesProperties properties) {
        return new RedisContainer(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
    }

    static class RedisDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(RedisDevServicesProperties.CONFIG_PREFIX, RedisDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("redis")
                    .description("Redis Dev Service")
                    .category(DevServiceCategories.REDIS)
                    .properties(properties)
                    .container(RedisContainer.class, () -> createContainer(properties))
                    .port(RedisContainer.REDIS_PORT)
                    .serviceConnectionName(COMPATIBLE_IMAGE_NAME)
                    .discovery(DataRedisConnectionDetails.class, RedisDiscoveredConnectionDetails::new));
        }

    }

}
