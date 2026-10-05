package io.arconia.dev.services.redis;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import io.arconia.dev.services.api.provider.DevServiceCategories;
import io.arconia.dev.services.api.provider.DevServiceProvider;
import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.autoconfigure.DevServicesAutoConfiguration;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.redis.RedisDevServicesAutoConfiguration.RedisDevServicesRegistrar;

/**
 * Auto-configuration for Redis Dev Services.
 */
@AutoConfiguration(after = DevServicesAutoConfiguration.class, before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("redis")
@EnableConfigurationProperties(RedisDevServicesProperties.class)
@Import(RedisDevServicesRegistrar.class)
public final class RedisDevServicesAutoConfiguration {

    @Bean
    DevServiceProvider redisDevServiceProvider() {
        return DevServiceProvider.of("redis", DevServiceCategories.REDIS);
    }

    static class RedisDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(RedisDevServicesProperties.CONFIG_PREFIX, RedisDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("redis")
                    .description("Redis Dev Service")
                    .properties(properties)
                    .container(ArconiaRedisContainer.class, () -> new ArconiaRedisContainer(properties))
                    .serviceConnectionName(ArconiaRedisContainer.COMPATIBLE_IMAGE_NAME)
                    .discovery(DataRedisConnectionDetails.class, RedisDiscoveredConnectionDetails::new));
        }

    }

}
