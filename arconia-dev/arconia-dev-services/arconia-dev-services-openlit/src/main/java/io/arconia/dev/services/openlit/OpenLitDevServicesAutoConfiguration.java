package io.arconia.dev.services.openlit;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import io.arconia.dev.services.api.provider.DevServiceCategories;
import io.arconia.dev.services.api.provider.DevServiceProvider;
import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.autoconfigure.DevServicesAutoConfiguration;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.openlit.OpenLitDevServicesAutoConfiguration.OpenLitDevServicesRegistrar;
import io.arconia.opentelemetry.autoconfigure.ConditionalOnOpenTelemetry;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.OpenTelemetryLoggingExporterProperties;

/**
 * Auto-configuration for OpenLit Dev Services.
 */
@AutoConfiguration(after = DevServicesAutoConfiguration.class, before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnOpenTelemetry
@ConditionalOnDevServicesEnabled("openlit")
@EnableConfigurationProperties(OpenLitDevServicesProperties.class)
@Import(OpenLitDevServicesRegistrar.class)
public final class OpenLitDevServicesAutoConfiguration {

    @Bean
    DevServiceProvider openLitDevServiceProvider() {
        return DevServiceProvider.of("openlit", DevServiceCategories.OPENTELEMETRY);
    }

    static class OpenLitDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(OpenLitDevServicesProperties.CONFIG_PREFIX, OpenLitDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("openlit")
                    .description("OpenLit Dev Service")
                    .container(container -> container
                            .type(ArconiaOpenLitContainer.class)
                            .supplier(() -> new ArconiaOpenLitContainer(properties))
                    ));

            // OpenLit does not support OpenTelemetry Logs, so we disable the export of Logs,
            // unless the developer has explicitly enabled them in the configuration.
            setDefaultProperty(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".type", "none");
        }

    }

}
