package io.arconia.dev.services.redis;

import com.redis.testcontainers.RedisContainer;
import com.redis.testcontainers.RedisStackContainer;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.redis.RedisDevServicesAutoConfiguration.RedisCommunityConfiguration;
import io.arconia.dev.services.redis.RedisDevServicesAutoConfiguration.RedisStackConfiguration;

/**
 * Auto-configuration for Redis Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnProperty(prefix = RedisDevServicesProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({RedisCommunityConfiguration.class, RedisStackConfiguration.class})
@EnableConfigurationProperties(RedisDevServicesProperties.class)
public class RedisDevServicesAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = RedisDevServicesProperties.CONFIG_PREFIX, name = "edition", havingValue = "community", matchIfMissing = true)
    static class RedisCommunityConfiguration {

        public static final String COMPATIBLE_IMAGE_NAME = "redis";

        @Configuration(proxyBeanMethods = false)
        @ConditionalOnClass(RestartScope.class)
        public static class ConfigurationWithRestart {

            @Bean
            @RestartScope
            @ServiceConnection
            @ConditionalOnMissingBean
            RedisContainer redisContainer(RedisDevServicesProperties properties) {
                return new RedisContainer(DockerImageName.parse(properties.getCommunity().getImageName())
                        .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                        .withReuse(properties.getCommunity().isReusable());
            }

        }

        @Configuration(proxyBeanMethods = false)
        @ConditionalOnMissingClass("org.springframework.boot.devtools.restart.RestartScope")
        public static class ConfigurationWithoutRestart {

            @Bean
            @ServiceConnection
            @ConditionalOnMissingBean
            RedisContainer postgresqlContainerNoRestartScope(RedisDevServicesProperties properties) {
                return new RedisContainer(DockerImageName.parse(properties.getCommunity().getImageName())
                        .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                        .withReuse(properties.getCommunity().isReusable());
            }

        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = RedisDevServicesProperties.CONFIG_PREFIX, name = "edition", havingValue = "stack")
    static class RedisStackConfiguration {

        public static final String COMPATIBLE_IMAGE_NAME = "redis/redis-stack-server";

        @Configuration(proxyBeanMethods = false)
        @ConditionalOnClass(RestartScope.class)
        public static class ConfigurationWithRestart {

            @Bean
            @RestartScope
            @ServiceConnection
            @ConditionalOnMissingBean
            RedisStackContainer redisContainer(RedisDevServicesProperties properties) {
                return new RedisStackContainer(DockerImageName.parse(properties.getStack().getImageName())
                        .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                        .withReuse(properties.getStack().isReusable());
            }

        }

        @Configuration(proxyBeanMethods = false)
        @ConditionalOnMissingClass("org.springframework.boot.devtools.restart.RestartScope")
        public static class ConfigurationWithoutRestart {

            @Bean
            @ServiceConnection
            @ConditionalOnMissingBean
            RedisStackContainer postgresqlContainerNoRestartScope(RedisDevServicesProperties properties) {
                return new RedisStackContainer(DockerImageName.parse(properties.getStack().getImageName())
                        .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                        .withReuse(properties.getStack().isReusable());
            }

        }

    }

}
