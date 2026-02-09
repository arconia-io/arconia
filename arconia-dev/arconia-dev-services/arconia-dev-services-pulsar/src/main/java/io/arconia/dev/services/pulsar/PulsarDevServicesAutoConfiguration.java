package io.arconia.dev.services.pulsar;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.autoconfigure.DevServicesAutoConfiguration;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.pulsar.PulsarDevServicesAutoConfiguration.PulsarDevServicesRegistrar;

/**
 * Auto-configuration for Pulsar Dev Services.
 */
@AutoConfiguration(after = DevServicesAutoConfiguration.class, before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("pulsar")
@EnableConfigurationProperties(PulsarDevServicesProperties.class)
@Import(PulsarDevServicesRegistrar.class)
public final class PulsarDevServicesAutoConfiguration {

    static class PulsarDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(PulsarDevServicesProperties.CONFIG_PREFIX, PulsarDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("pulsar")
                    .description("Pulsar Dev Service")
                    .container(container -> container
                            .type(ArconiaPulsarContainer.class)
                            .supplier(() -> new ArconiaPulsarContainer(properties))
                    )
            );
        }

    }

}
