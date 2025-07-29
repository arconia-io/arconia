package io.arconia.dev.services.ollama;

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
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.ollama.OllamaDevServicesAutoConfiguration.ConfigurationWithRestart;
import io.arconia.dev.services.ollama.OllamaDevServicesAutoConfiguration.ConfigurationWithoutRestart;

/**
 * Auto-configuration for Ollama Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnProperty(prefix = OllamaDevServicesProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true")
@EnableConfigurationProperties(OllamaDevServicesProperties.class)
@Import({ConfigurationWithRestart.class, ConfigurationWithoutRestart.class})
public final class OllamaDevServicesAutoConfiguration {

    public static final String COMPATIBLE_IMAGE_NAME = "ollama/ollama";

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RestartScope.class)
    public static final class ConfigurationWithRestart {

        @Bean
        @RestartScope
        @ServiceConnection
        @ConditionalOnMissingBean
        OllamaContainer ollamaContainer(OllamaDevServicesProperties properties) {
            return new OllamaContainer(DockerImageName.parse(properties.getImageName())
                    .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                    .withEnv(properties.getEnvironment())
                    .withStartupTimeout(properties.getStartupTimeout())
                    .withReuse(properties.getShared().asBoolean());
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.springframework.boot.devtools.restart.RestartScope")
    public static final class ConfigurationWithoutRestart {

        @Bean
        @ServiceConnection
        @ConditionalOnMissingBean
        OllamaContainer ollamaContainerNoRestartScope(OllamaDevServicesProperties properties) {
            return new OllamaContainer(DockerImageName.parse(properties.getImageName())
                    .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                    .withEnv(properties.getEnvironment())
                    .withStartupTimeout(properties.getStartupTimeout())
                    .withReuse(properties.getShared().asBoolean());
        }

    }

}
