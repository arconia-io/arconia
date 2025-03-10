package io.arconia.dev.service.ollama;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Auto-configuration for Ollama Dev Service.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnProperty(prefix = OllamaDevServiceProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true")
@EnableConfigurationProperties(OllamaDevServiceProperties.class)
public class OllamaDevServiceAutoConfiguration {

    public static final String COMPATIBLE_IMAGE_NAME = "ollama/ollama";

    @Bean
    @RestartScope
    @ServiceConnection
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "spring.devtools.restart", name = "enabled", havingValue = "true", matchIfMissing = true)
    OllamaContainer ollamaContainer(OllamaDevServiceProperties properties) {
        return new OllamaContainer(DockerImageName.parse(properties.getImageName())
                .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                .withReuse(properties.isReusable());
    }

    @Bean
    @ServiceConnection
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "spring.devtools.restart", name = "enabled", havingValue = "false")
    OllamaContainer ollamaContainerNoRestartScope(OllamaDevServiceProperties properties) {
        return new OllamaContainer(DockerImageName.parse(properties.getImageName())
                .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                .withReuse(properties.isReusable());
    }


}
