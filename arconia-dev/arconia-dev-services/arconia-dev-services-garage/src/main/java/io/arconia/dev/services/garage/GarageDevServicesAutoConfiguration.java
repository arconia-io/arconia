package io.arconia.dev.services.garage;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.autoconfigure.DevServicesAutoConfiguration;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.garage.GarageDevServicesAutoConfiguration.GarageDevServicesRegistrar;

/**
 * Auto-configuration for Garage Dev Services.
 */
@AutoConfiguration(after = DevServicesAutoConfiguration.class, before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("garage")
@EnableConfigurationProperties(GarageDevServicesProperties.class)
@Import(GarageDevServicesRegistrar.class)
public final class GarageDevServicesAutoConfiguration {

    private GarageDevServicesAutoConfiguration() {}

    static class GarageDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(GarageDevServicesProperties.CONFIG_PREFIX, GarageDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("garage")
                    .description("Garage Dev Service")
                    .container(container -> container
                            .type(ArconiaGarageContainer.class)
                            .serviceConnectionName("garage")
                            .supplier(() -> new ArconiaGarageContainer(properties))
                    ));
        }

    }

}
