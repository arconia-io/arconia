package io.arconia.dev.services.artemis;

import org.springframework.boot.artemis.autoconfigure.ArtemisConnectionDetails;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.testcontainers.activemq.ArtemisContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.artemis.ArtemisDevServicesAutoConfiguration.ArtemisDevServicesRegistrar;
import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;

/**
 * Auto-configuration for ActiveMQ Artemis Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("artemis")
@EnableConfigurationProperties(ArtemisDevServicesProperties.class)
@Import(ArtemisDevServicesRegistrar.class)
public final class ArtemisDevServicesAutoConfiguration {

    static final String COMPATIBLE_IMAGE_NAME = "apache/activemq-artemis";

    // CORE, MQTT, AMQP, HORNETQ, STOMP, OPENWIRE
    static final int TCP_PORT = 61616;

    static final int WEB_CONSOLE_PORT = 8161;

    static ArtemisContainer createContainer(ArtemisDevServicesProperties properties) {
        var container = new ArtemisContainer(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        container.withUser(properties.getUsername());
        container.withPassword(properties.getPassword());
        return container;
    }

    static class ArtemisDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(ArtemisDevServicesProperties.CONFIG_PREFIX, ArtemisDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("artemis")
                    .description("Artemis Dev Service")
                    .properties(properties)
                    .container(ArtemisContainer.class, () -> createContainer(properties))
                    .port(TCP_PORT)
                    .additionalPort(WEB_CONSOLE_PORT, properties.getManagementConsolePort())
                    .discovery(ArtemisConnectionDetails.class,
                            container -> new ArtemisDiscoveredConnectionDetails(container, properties))
                    .link("Artemis Management Console", WEB_CONSOLE_PORT, "/console"));
        }

    }

}
