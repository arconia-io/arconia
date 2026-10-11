package io.arconia.dev.services.kafka;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.autoconfigure.DevServicesAutoConfiguration;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.kafka.KafkaDevServicesAutoConfiguration.KafkaDevServicesRegistrar;

/**
 * Auto-configuration for Kafka Dev Services.
 */
@AutoConfiguration(after = DevServicesAutoConfiguration.class, before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("kafka")
@EnableConfigurationProperties(KafkaDevServicesProperties.class)
@Import(KafkaDevServicesRegistrar.class)
public final class KafkaDevServicesAutoConfiguration {

    static final String COMPATIBLE_IMAGE_NAME = "apache/kafka-native";

    static final int KAFKA_PORT = 9092;

    static final String READY_REGEX = ".*Transitioning from RECOVERY to RUNNING.*";

    static KafkaContainer createContainer(KafkaDevServicesProperties properties) {
        var container = new KafkaContainer(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        // KafkaContainer waits on a wait strategy instance shared across all Kafka containers,
        // so when we customize the startup timeout, it will be applied to all of them.
        // Hence, we must provide an equivalent wait strategy of our own.
        container.waitingFor(Wait.forLogMessage(READY_REGEX, 1));
        return container;
    }

    static class KafkaDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(KafkaDevServicesProperties.CONFIG_PREFIX, KafkaDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("kafka")
                    .description("Kafka Dev Service")
                    .properties(properties)
                    .container(KafkaContainer.class, () -> createContainer(properties))
                    .port(KAFKA_PORT)
                    .discovery(KafkaConnectionDetails.class, KafkaDiscoveredConnectionDetails::new));
        }

    }

}
