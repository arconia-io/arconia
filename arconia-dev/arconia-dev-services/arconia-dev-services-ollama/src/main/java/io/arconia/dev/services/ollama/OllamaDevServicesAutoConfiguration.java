package io.arconia.dev.services.ollama;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.config.DevServicesBeanRegistrations;

/**
 * Auto-configuration for Ollama Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnBooleanProperty(prefix = "arconia.dev.services.ollama", name = "enabled")
@EnableConfigurationProperties(OllamaDevServicesProperties.class)
public final class OllamaDevServicesAutoConfiguration {

    private static final String COMPATIBLE_IMAGE_NAME = "ollama/ollama";

    @Bean
    @ServiceConnection
    @ConditionalOnMissingBean
    OllamaContainer ollamaContainer(OllamaDevServicesProperties properties) {
        return new ArconiaOllamaContainer(DockerImageName.parse(properties.getImageName())
                .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                .withEnv(properties.getEnvironment())
                .withStartupTimeout(properties.getStartupTimeout())
                .withReuse(properties.getShared().asBoolean());
    }

    @Bean
    static BeanFactoryPostProcessor ollamaContainerPostProcessor() {
        return DevServicesBeanRegistrations.beanFactoryPostProcessor(OllamaContainer.class);
    }

}
