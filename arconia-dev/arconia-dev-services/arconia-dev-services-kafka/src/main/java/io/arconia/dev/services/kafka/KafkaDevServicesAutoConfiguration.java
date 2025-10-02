package io.arconia.dev.services.kafka;

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
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.kafka.KafkaDevServicesAutoConfiguration.ConfigurationWithRestart;
import io.arconia.dev.services.kafka.KafkaDevServicesAutoConfiguration.ConfigurationWithoutRestart;

/**
 * Auto-configuration for Kafka Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnProperty(prefix = "arconia.dev.services.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(KafkaDevServicesProperties.class)
@Import({ConfigurationWithRestart.class, ConfigurationWithoutRestart.class})
public final class KafkaDevServicesAutoConfiguration {

    public static final String COMPATIBLE_IMAGE_NAME = "apache/kafka-native";

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RestartScope.class)
    public static final class ConfigurationWithRestart {

        @Bean
        @RestartScope
        @ServiceConnection
        @ConditionalOnMissingBean
        KafkaContainer kafkaContainer(KafkaDevServicesProperties properties) {
            return new KafkaContainer(DockerImageName.parse(properties.getImageName())
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
        KafkaContainer kafkaContainerNoRestartScope(KafkaDevServicesProperties properties) {
            return new KafkaContainer(DockerImageName.parse(properties.getImageName())
                    .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                    .withEnv(properties.getEnvironment())
                    .withStartupTimeout(properties.getStartupTimeout())
                    .withReuse(properties.getShared().asBoolean());
        }

    }

}
