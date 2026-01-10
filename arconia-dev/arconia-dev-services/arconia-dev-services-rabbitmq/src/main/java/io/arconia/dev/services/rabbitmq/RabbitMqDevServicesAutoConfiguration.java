package io.arconia.dev.services.rabbitmq;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.config.DevServicesBeanRegistrations;

/**
 * Auto-configuration for RabbitMQ Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnBooleanProperty(prefix = "arconia.dev.services.rabbitmq", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(RabbitMqDevServicesProperties.class)
public final class RabbitMqDevServicesAutoConfiguration {

    private static final String COMPATIBLE_IMAGE_NAME = "rabbitmq";

    @Bean
    @ServiceConnection
    @ConditionalOnMissingBean
    RabbitMQContainer rabbitmqContainer(RabbitMqDevServicesProperties properties) {
        return new ArconiaRabbitMqContainer(DockerImageName.parse(properties.getImageName())
                .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                .withEnv(properties.getEnvironment())
                .withStartupTimeout(properties.getStartupTimeout())
                .withReuse(properties.getShared().asBoolean());
    }

    @Bean
    static BeanFactoryPostProcessor rabbitmqContainerPostProcessor() {
        return DevServicesBeanRegistrations.beanFactoryPostProcessor(RabbitMQContainer.class);
    }

}
