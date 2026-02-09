package io.arconia.dev.services.phoenix;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.autoconfigure.DevServicesAutoConfiguration;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.phoenix.PhoenixDevServicesAutoConfiguration.PhoenixDevServicesRegistrar;
import io.arconia.opentelemetry.autoconfigure.ConditionalOnOpenTelemetry;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.OpenTelemetryLoggingExporterProperties;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.OpenTelemetryMetricsExporterProperties;

/**
 * Auto-configuration for Arize Phoenix Dev Services.
 */
@AutoConfiguration(after = DevServicesAutoConfiguration.class, before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("phoenix")
@ConditionalOnOpenTelemetry
@EnableConfigurationProperties(PhoenixDevServicesProperties.class)
@Import(PhoenixDevServicesRegistrar.class)
public final class PhoenixDevServicesAutoConfiguration {

    static class PhoenixDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(PhoenixDevServicesProperties.CONFIG_PREFIX, PhoenixDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("phoenix")
                    .description("Phoenix Dev Service")
                    .container(container -> container
                            .type(ArconiaPhoenixContainer.class)
                            .serviceConnectionName("phoenix")
                            .supplier(() -> new ArconiaPhoenixContainer(properties))
                    ));

            // Phoenix supports only OpenTelemetry Traces, so we disable the export of Logs and Metrics,
            // unless the developer has explicitly enabled them in the configuration.
            setDefaultProperty(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".type", "none");
            setDefaultProperty(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".type", "none");
        }

    }

}
