package io.arconia.dev.services.redis;

import com.redis.testcontainers.RedisContainer;
import com.redis.testcontainers.RedisStackContainer;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.testcontainers.utility.DockerImageName;

/**
 * Auto-configuration for Redis Dev Service.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnProperty(prefix = RedisDevServiceProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({RedisDevServiceAutoConfiguration.RedisCommunityConfiguration.class, RedisDevServiceAutoConfiguration.RedisStackConfiguration.class})
@EnableConfigurationProperties(RedisDevServiceProperties.class)
public class RedisDevServiceAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = RedisDevServiceProperties.CONFIG_PREFIX, name = "edition", havingValue = "community", matchIfMissing = true)
    static class RedisCommunityConfiguration {

        public static final String COMPATIBLE_IMAGE_NAME = "redis";

        @Bean
        @RestartScope
        @ServiceConnection
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "spring.devtools.restart", name = "enabled", havingValue = "true", matchIfMissing = true)
        RedisContainer redisContainer(RedisDevServiceProperties properties) {
            return new RedisContainer(DockerImageName.parse(properties.getCommunity().getImageName())
                    .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                    .withReuse(properties.getCommunity().isReusable());
        }

        @Bean
        @ServiceConnection
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "spring.devtools.restart", name = "enabled", havingValue = "false")
        RedisContainer postgresqlContainerNoRestartScope(RedisDevServiceProperties properties) {
            return new RedisContainer(DockerImageName.parse(properties.getCommunity().getImageName())
                    .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                    .withReuse(properties.getCommunity().isReusable());
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = RedisDevServiceProperties.CONFIG_PREFIX, name = "edition", havingValue = "stack")
    static class RedisStackConfiguration {

        public static final String COMPATIBLE_IMAGE_NAME = "redis/redis-stack-server";

        @Bean
        @RestartScope
        @ServiceConnection
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "spring.devtools.restart", name = "enabled", havingValue = "true", matchIfMissing = true)
        RedisStackContainer redisContainer(RedisDevServiceProperties properties) {
            return new RedisStackContainer(DockerImageName.parse(properties.getStack().getImageName())
                    .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                    .withReuse(properties.getStack().isReusable());
        }

        @Bean
        @ServiceConnection
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "spring.devtools.restart", name = "enabled", havingValue = "false")
        RedisStackContainer postgresqlContainerNoRestartScope(RedisDevServiceProperties properties) {
            return new RedisStackContainer(DockerImageName.parse(properties.getStack().getImageName())
                    .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                    .withReuse(properties.getStack().isReusable());
        }

    }

}
