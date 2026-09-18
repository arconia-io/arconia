package io.arconia.dev.services.keycloak;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.autoconfigure.DevServicesAutoConfiguration;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.keycloak.KeycloakDevServicesAutoConfiguration.KeycloakDevServicesRegistrar;

/**
 * Auto-configuration for Keycloak Dev Services.
 */
@AutoConfiguration(after = DevServicesAutoConfiguration.class, before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("keycloak")
@EnableConfigurationProperties(KeycloakDevServicesProperties.class)
@Import(KeycloakDevServicesRegistrar.class)
public final class KeycloakDevServicesAutoConfiguration {

    static class KeycloakDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(KeycloakDevServicesProperties.CONFIG_PREFIX, KeycloakDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("keycloak")
                    .description("Keycloak Dev Service")
                    .properties(properties)
                    .container(container -> container
                            .type(ArconiaKeycloakContainer.class)
                            .supplier(() -> new ArconiaKeycloakContainer(properties))
                    )
                    .discovery(discovery -> discovery
                            .connectionDetails(KeycloakConnectionDetails.class,
                                    container -> new KeycloakDiscoveredConnectionDetails(container, properties))
                    ));
        }

    }

}
