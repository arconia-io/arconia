package io.arconia.dev.services.redis;

import com.redis.testcontainers.RedisContainer;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.redis.RedisDevServicesAutoConfiguration.ConfigurationWithRestart;
import io.arconia.dev.services.redis.RedisDevServicesAutoConfiguration.ConfigurationWithoutRestart;

/**
 * Auto-configuration for Redis Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnProperty(prefix = RedisDevServicesProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({ConfigurationWithRestart.class, ConfigurationWithoutRestart.class})
@EnableConfigurationProperties(RedisDevServicesProperties.class)
public class RedisDevServicesAutoConfiguration {

    public static final String COMPATIBLE_IMAGE_NAME = "redis";

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RestartScope.class)
    public static class ConfigurationWithRestart {

        @Bean
        @RestartScope
        @ServiceConnection
        @ConditionalOnMissingBean
        RedisContainer redisContainer(RedisDevServicesProperties properties, ApplicationContext applicationContext) {
            return new RedisContainer(DockerImageName.parse(properties.getImageName())
                    .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                    .withEnv(properties.getEnvironment())
                    .withReuse(properties.getShared().asBoolean(applicationContext));
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.springframework.boot.devtools.restart.RestartScope")
    public static class ConfigurationWithoutRestart {

        @Bean
        @ServiceConnection
        @ConditionalOnMissingBean
        RedisContainer redisContainerNoRestartScope(RedisDevServicesProperties properties, ApplicationContext applicationContext) {
            return new RedisContainer(DockerImageName.parse(properties.getImageName())
                    .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                    .withEnv(properties.getEnvironment())
                    .withReuse(properties.getShared().asBoolean(applicationContext));
        }

    }

}
