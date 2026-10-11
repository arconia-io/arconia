package io.arconia.dev.services.rabbitmq;

import org.springframework.boot.amqp.autoconfigure.RabbitConnectionDetails;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

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

    static final String COMPATIBLE_IMAGE_NAME = "rabbitmq";

    static final int AMQP_PORT = 5672;

    static final int HTTP_PORT = 15672;

    static RabbitMQContainer createContainer(RabbitMqDevServicesProperties properties) {
        var container = new RabbitMQContainer(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        container.withAdminUser(properties.getUsername());
        container.withAdminPassword(properties.getPassword());
        return container;
    }

    static class RabbitMqDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(RabbitMqDevServicesProperties.CONFIG_PREFIX, RabbitMqDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("rabbitmq")
                    .description("RabbitMQ Dev Service")
                    .properties(properties)
                    .container(RabbitMQContainer.class, () -> createContainer(properties))
                    .port(AMQP_PORT)
                    .additionalPort(HTTP_PORT, properties.getManagementConsolePort())
                    .discovery(RabbitConnectionDetails.class,
                            container -> new RabbitMqDiscoveredConnectionDetails(container, properties))
                    .link("RabbitMQ Management Console", HTTP_PORT));
        }

    }

}
