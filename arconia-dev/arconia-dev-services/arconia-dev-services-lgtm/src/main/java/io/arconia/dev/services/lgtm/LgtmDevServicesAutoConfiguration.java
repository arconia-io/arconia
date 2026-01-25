package io.arconia.dev.services.lgtm;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.autoconfigure.DevServicesAutoConfiguration;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.lgtm.LgtmDevServicesAutoConfiguration.LgtmDevServicesRegistrar;
import io.arconia.opentelemetry.autoconfigure.ConditionalOnOpenTelemetry;

/**
 * Auto-configuration for Grafana LGTM Dev Services.
 */
@AutoConfiguration(after = DevServicesAutoConfiguration.class, before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnOpenTelemetry
@ConditionalOnDevServicesEnabled("lgtm")
@EnableConfigurationProperties(LgtmDevServicesProperties.class)
@Import(LgtmDevServicesRegistrar.class)
public final class LgtmDevServicesAutoConfiguration {

    static class LgtmDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(LgtmDevServicesProperties.CONFIG_PREFIX, LgtmDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("lgtm")
                    .description("Grafana LGTM Dev Service")
                    .container(container -> container
                            .type(ArconiaLgtmStackContainer.class)
                            .supplier(() -> new ArconiaLgtmStackContainer(properties)
                                    .withEnv(properties.getEnvironment())
                                    .withNetworkAliases(properties.getNetworkAliases().toArray(new String[]{}))
                                    .withStartupTimeout(properties.getStartupTimeout())
                                    .withReuse(isDevMode() && properties.isShared()))
                    ));
        }

    }

}
