package io.arconia.dev.services.artemis;

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
import org.springframework.util.StringUtils;
import org.testcontainers.activemq.ArtemisContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.artemis.ArtemisDevServicesAutoConfiguration.ConfigurationWithRestart;
import io.arconia.dev.services.artemis.ArtemisDevServicesAutoConfiguration.ConfigurationWithoutRestart;

/**
 * Auto-configuration for ActiveMQ Artemis Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnProperty(prefix = "arconia.dev.services.artemis", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ArtemisDevServicesProperties.class)
@Import({ConfigurationWithRestart.class, ConfigurationWithoutRestart.class})
public final class ArtemisDevServicesAutoConfiguration {

    public static final String COMPATIBLE_IMAGE_NAME = "apache/activemq-artemis";

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RestartScope.class)
    public static final class ConfigurationWithRestart {

        @Bean
        @RestartScope
        @ServiceConnection
        @ConditionalOnMissingBean
        ArtemisContainer artemisContainer(ArtemisDevServicesProperties properties) {
            return new ArtemisContainer(DockerImageName.parse(properties.getImageName())
                    .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                    .withEnv(properties.getEnvironment())
                    .withStartupTimeout(properties.getStartupTimeout())
                    .withReuse(properties.getShared().asBoolean())
                    .withUser(StringUtils.hasText(properties.getUsername()) ? properties.getUsername() : ArtemisDevServicesProperties.DEFAULT_USERNAME)
                    .withPassword(StringUtils.hasText(properties.getPassword()) ? properties.getPassword() : ArtemisDevServicesProperties.DEFAULT_PASSWORD);
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.springframework.boot.devtools.restart.RestartScope")
    public static final class ConfigurationWithoutRestart {

        @Bean
        @ServiceConnection
        @ConditionalOnMissingBean
        ArtemisContainer artemisContainerNoRestartScope(ArtemisDevServicesProperties properties) {
            return new ArtemisContainer(DockerImageName.parse(properties.getImageName())
                    .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                    .withEnv(properties.getEnvironment())
                    .withStartupTimeout(properties.getStartupTimeout())
                    .withReuse(properties.getShared().asBoolean())
                    .withUser(StringUtils.hasText(properties.getUsername()) ? properties.getUsername() : ArtemisDevServicesProperties.DEFAULT_USERNAME)
                    .withPassword(StringUtils.hasText(properties.getPassword()) ? properties.getPassword() : ArtemisDevServicesProperties.DEFAULT_PASSWORD);
        }

    }

}
