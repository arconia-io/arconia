package io.arconia.dev.services.artemis;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import io.arconia.dev.services.artemis.ArtemisDevServicesAutoConfiguration.ArtemisDevServicesRegistrar;
import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.autoconfigure.DevServicesAutoConfiguration;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;

/**
 * Auto-configuration for ActiveMQ Artemis Dev Services.
 */
@AutoConfiguration(after = DevServicesAutoConfiguration.class, before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("artemis")
@EnableConfigurationProperties(ArtemisDevServicesProperties.class)
@Import(ArtemisDevServicesRegistrar.class)
public final class ArtemisDevServicesAutoConfiguration {

    static class ArtemisDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(ArtemisDevServicesProperties.CONFIG_PREFIX, ArtemisDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("artemis")
                    .description("Artemis Dev Service")
                    .container(container -> container
                            .type(ArconiaArtemisContainer.class)
                            .supplier(() -> new ArconiaArtemisContainer(properties)
                                    .withEnv(properties.getEnvironment())
                                    .withNetworkAliases(properties.getNetworkAliases().toArray(new String[]{}))
                                    .withStartupTimeout(properties.getStartupTimeout())
                                    .withReuse(isDevMode() && properties.isShared())
                                    .withUser(StringUtils.hasText(properties.getUsername()) ? properties.getUsername() : ArtemisDevServicesProperties.DEFAULT_USERNAME)
                                    .withPassword(StringUtils.hasText(properties.getPassword()) ? properties.getPassword() : ArtemisDevServicesProperties.DEFAULT_PASSWORD))
                    ));

        }

    }

}
