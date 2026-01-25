package io.arconia.dev.services.valkey;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.autoconfigure.DevServicesAutoConfiguration;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.valkey.ValkeyDevServicesAutoConfiguration.ValkeyDevServicesRegistrar;

/**
 * Auto-configuration for Valkey Dev Services.
 */
@AutoConfiguration(after = DevServicesAutoConfiguration.class, before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("valkey")
@EnableConfigurationProperties(ValkeyDevServicesProperties.class)
@Import(ValkeyDevServicesRegistrar.class)
public final class ValkeyDevServicesAutoConfiguration {

    static class ValkeyDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(ValkeyDevServicesProperties.CONFIG_PREFIX, ValkeyDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("valkey")
                    .description("Valkey Dev Service")
                    .container(container -> container
                            .type(ArconiaValkeyContainer.class)
                            .serviceConnectionName("redis")
                            .supplier(() -> new ArconiaValkeyContainer(properties)
                                    .withEnv(properties.getEnvironment())
                                    .withNetworkAliases(properties.getNetworkAliases().toArray(new String[]{}))
                                    .withStartupTimeout(properties.getStartupTimeout())
                                    .withReuse(isDevMode() && properties.isShared()))
                    ));
        }

    }

}
