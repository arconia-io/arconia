package io.arconia.dev.services.redis;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.config.DevServicesBeanRegistrations;
import io.arconia.testcontainers.redis.RedisContainer;

/**
 * Auto-configuration for Redis Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnBooleanProperty(prefix = "arconia.dev.services.redis", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(RedisDevServicesProperties.class)
public final class RedisDevServicesAutoConfiguration {

    private static final String COMPATIBLE_IMAGE_NAME = "redis";

    @Bean
    @ServiceConnection("redis")
    @ConditionalOnMissingBean
    RedisContainer redisContainer(RedisDevServicesProperties properties) {
        return new ArconiaRedisContainer(DockerImageName.parse(properties.getImageName())
                .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                .withEnv(properties.getEnvironment())
                .withStartupTimeout(properties.getStartupTimeout())
                .withReuse(properties.getShared().asBoolean());
    }

    @Bean
    static BeanFactoryPostProcessor redisContainerPostProcessor() {
        return DevServicesBeanRegistrations.beanFactoryPostProcessor(RedisContainer.class);
    }

}
