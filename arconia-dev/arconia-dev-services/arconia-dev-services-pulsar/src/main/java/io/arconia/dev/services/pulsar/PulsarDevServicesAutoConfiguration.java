package io.arconia.dev.services.pulsar;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.pulsar.autoconfigure.PulsarConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.testcontainers.pulsar.PulsarContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.pulsar.PulsarDevServicesAutoConfiguration.PulsarDevServicesRegistrar;

/**
 * Auto-configuration for Pulsar Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("pulsar")
@EnableConfigurationProperties(PulsarDevServicesProperties.class)
@Import(PulsarDevServicesRegistrar.class)
public final class PulsarDevServicesAutoConfiguration {

    static final String COMPATIBLE_IMAGE_NAME = "apachepulsar/pulsar";

    static PulsarContainer createContainer(PulsarDevServicesProperties properties) {
        return new PulsarContainer(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
    }

    static class PulsarDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(PulsarDevServicesProperties.CONFIG_PREFIX, PulsarDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("pulsar")
                    .description("Pulsar Dev Service")
                    .properties(properties)
                    .container(PulsarContainer.class, () -> createContainer(properties))
                    .port(PulsarContainer.BROKER_PORT)
                    .additionalPort(PulsarContainer.BROKER_HTTP_PORT, properties.getAdminPort())
                    .discovery(PulsarConnectionDetails.class, PulsarDiscoveredConnectionDetails::new)
                    .link("Pulsar Admin API", PulsarContainer.BROKER_HTTP_PORT));
        }

    }

}
