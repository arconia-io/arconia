package io.arconia.dev.services.openlit;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.registration.DevServiceCategories;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.openlit.OpenLitDevServicesAutoConfiguration.OpenLitDevServicesRegistrar;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.OtlpConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.ConditionalOnOpenTelemetryLoggingExporter;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.ConditionalOnOpenTelemetryMetricsExporter;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.ConditionalOnOpenTelemetryTracingExporter;
import io.arconia.testcontainers.openlit.OpenLitContainer;

/**
 * Auto-configuration for OpenLIT Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@Conditional(OpenLitDevServicesAutoConfiguration.OtlpExportEnabled.class)
@ConditionalOnDevServicesEnabled("openlit")
@EnableConfigurationProperties(OpenLitDevServicesProperties.class)
@Import(OpenLitDevServicesRegistrar.class)
public final class OpenLitDevServicesAutoConfiguration {

    static final String COMPATIBLE_IMAGE_NAME = "ghcr.io/openlit/openlit";

    static OpenLitContainer createContainer(OpenLitDevServicesProperties properties) {
        var container = new OpenLitContainer(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        container.withClickHouseImage(properties.getClickhouseImageName());
        return container;
    }

    /**
     * The dev service is only started when the application exports at least one signal via OTLP,
     * since it would receive nothing otherwise, as it is the case in tests by default.
     */
    static class OtlpExportEnabled extends AnyNestedCondition {

        OtlpExportEnabled() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @ConditionalOnOpenTelemetryLoggingExporter("otlp")
        static class LogsExportEnabled {}

        @ConditionalOnOpenTelemetryMetricsExporter("otlp")
        static class MetricsExportEnabled {}

        @ConditionalOnOpenTelemetryTracingExporter("otlp")
        static class TracesExportEnabled {}

    }

    static class OpenLitDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(OpenLitDevServicesProperties.CONFIG_PREFIX, OpenLitDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("openlit")
                    .description("OpenLIT Dev Service")
                    .category(DevServiceCategories.OPENTELEMETRY)
                    .properties(properties)
                    .container(OpenLitContainer.class, () -> createContainer(properties))
                    .port(OpenLitContainer.UI_PORT)
                    .additionalPort(OpenLitContainer.OTLP_GRPC_PORT, properties.getOtlpGrpcPort())
                    .additionalPort(OpenLitContainer.OTLP_HTTP_PORT, properties.getOtlpHttpPort())
                    .discovery(OtlpConnectionDetails.class, OpenLitDiscoveredConnectionDetails::new)
                    .link("OpenLIT UI", OpenLitContainer.UI_PORT));
        }

    }

}
