package io.arconia.dev.services.phoenix;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.autoconfigure.DevServicesAutoConfiguration;
import io.arconia.dev.services.core.registration.DevServiceCategories;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.phoenix.PhoenixDevServicesAutoConfiguration.PhoenixDevServicesRegistrar;
import io.arconia.opentelemetry.autoconfigure.ConditionalOnOpenTelemetry;
import io.arconia.opentelemetry.autoconfigure.exporter.OpenTelemetryExporterAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.OpenTelemetryLoggingExporterProperties;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.OpenTelemetryMetricsExporterProperties;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingConnectionDetails;
import io.arconia.testcontainers.phoenix.PhoenixContainer;

/**
 * Auto-configuration for Arize Phoenix Dev Services.
 */
@AutoConfiguration(after = DevServicesAutoConfiguration.class,
        before = { OpenTelemetryExporterAutoConfiguration.class, ServiceConnectionAutoConfiguration.class })
@ConditionalOnDevServicesEnabled("phoenix")
@ConditionalOnOpenTelemetry
@EnableConfigurationProperties(PhoenixDevServicesProperties.class)
@Import(PhoenixDevServicesRegistrar.class)
public final class PhoenixDevServicesAutoConfiguration {

    static final String COMPATIBLE_IMAGE_NAME = "arizephoenix/phoenix";

    static PhoenixContainer createContainer(PhoenixDevServicesProperties properties) {
        return new PhoenixContainer(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
    }

    static class PhoenixDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(PhoenixDevServicesProperties.CONFIG_PREFIX, PhoenixDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("phoenix")
                    .description("Phoenix Dev Service")
                    .category(DevServiceCategories.OPENTELEMETRY)
                    .properties(properties)
                    .container(PhoenixContainer.class, () -> createContainer(properties))
                    .port(PhoenixContainer.HTTP_PORT)
                    .additionalPort(PhoenixContainer.GRPC_PORT, properties.getOtlpGrpcPort())
                    .discovery(OtlpTracingConnectionDetails.class, PhoenixDiscoveredConnectionDetails::new)
                    .link("Phoenix UI", PhoenixContainer.HTTP_PORT));

            // Phoenix accepts traces only. Logs and metrics are not exported while the dev service is active.
            addDynamicProperty(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".type", () -> "none");
            addDynamicProperty(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".type", () -> "none");
        }

    }

}
