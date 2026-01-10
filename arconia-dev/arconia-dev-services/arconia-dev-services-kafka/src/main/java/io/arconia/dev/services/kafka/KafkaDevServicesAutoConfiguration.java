package io.arconia.dev.services.kafka;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.config.DevServicesBeanRegistrations;

/**
 * Auto-configuration for Kafka Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnBooleanProperty(prefix = "arconia.dev.services.kafka", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(KafkaDevServicesProperties.class)
public final class KafkaDevServicesAutoConfiguration {

    private static final String COMPATIBLE_IMAGE_NAME = "apache/kafka-native";

    @Bean
    @ServiceConnection
    @ConditionalOnMissingBean
    KafkaContainer kafkaContainer(KafkaDevServicesProperties properties) {
        return new ArconiaKafkaContainer(DockerImageName.parse(properties.getImageName())
                .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                .withEnv(properties.getEnvironment())
                .withStartupTimeout(properties.getStartupTimeout())
                .withReuse(properties.getShared().asBoolean());
    }

    @Bean
    static BeanFactoryPostProcessor kafkaContainerPostProcessor() {
        return DevServicesBeanRegistrations.beanFactoryPostProcessor(KafkaContainer.class);
    }

}
