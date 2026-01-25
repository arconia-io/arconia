package io.arconia.dev.services.rabbitmq;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.autoconfigure.DevServicesAutoConfiguration;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.rabbitmq.RabbitMqDevServicesAutoConfiguration.RabbitMqDevServicesRegistrar;

/**
 * Auto-configuration for RabbitMQ Dev Services.
 */
@AutoConfiguration(after = DevServicesAutoConfiguration.class, before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("rabbitmq")
@EnableConfigurationProperties(RabbitMqDevServicesProperties.class)
@Import(RabbitMqDevServicesRegistrar.class)
public final class RabbitMqDevServicesAutoConfiguration {

    static class RabbitMqDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(RabbitMqDevServicesProperties.CONFIG_PREFIX, RabbitMqDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("rabbitmq")
                    .description("RabbitMQ Dev Service")
                    .container(container -> container
                            .type(ArconiaRabbitMqContainer.class)
                            .supplier(() -> new ArconiaRabbitMqContainer(properties)
                                    .withEnv(properties.getEnvironment())
                                    .withNetworkAliases(properties.getNetworkAliases().toArray(new String[]{}))
                                    .withStartupTimeout(properties.getStartupTimeout())
                                    .withReuse(isDevMode() && properties.isShared()))
                    )
            );
        }

    }

}
