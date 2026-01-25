package io.arconia.dev.services.kafka;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

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

    static class KafkaDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(KafkaDevServicesProperties.CONFIG_PREFIX, KafkaDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("kafka")
                    .description("Kafka Dev Service")
                    .container(container -> container
                            .type(ArconiaKafkaContainer.class)
                            .supplier(() -> new ArconiaKafkaContainer(properties)
                                    .withEnv(properties.getEnvironment())
                                    .withNetworkAliases(properties.getNetworkAliases().toArray(new String[]{}))
                                    .withStartupTimeout(properties.getStartupTimeout())
                                    .withReuse(isDevMode() && properties.isShared()))
                    ));
        }

    }

}
