package io.arconia.dev.services.lldap;

import io.arconia.dev.services.core.autoconfigure.DevServicesAutoConfiguration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.lldap.LldapDevServicesAutoConfiguration.ArtemisDevServicesRegistrar;

/**
 * Auto-configuration for LLDAP Dev Services.
 */
@AutoConfiguration(after = DevServicesAutoConfiguration.class, before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("lldap")
@EnableConfigurationProperties(LldapDevServicesProperties.class)
@Import(ArtemisDevServicesRegistrar.class)
public final class LldapDevServicesAutoConfiguration {

    static class ArtemisDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(LldapDevServicesProperties.CONFIG_PREFIX, LldapDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("lldap")
                    .description("LLDAP Dev Service")
                    .container(container -> container
                            .type(ArconiaLldapContainer.class)
                            .supplier(() -> new ArconiaLldapContainer(properties)
                                    .withEnv(properties.getEnvironment())
                                    .withNetworkAliases(properties.getNetworkAliases().toArray(new String[]{}))
                                    .withStartupTimeout(properties.getStartupTimeout())
                                    .withReuse(isDevMode() && properties.isShared()))
                    ));
        }

    }

}
