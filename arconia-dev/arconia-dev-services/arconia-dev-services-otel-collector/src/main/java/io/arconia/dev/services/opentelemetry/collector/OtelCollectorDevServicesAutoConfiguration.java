package io.arconia.dev.services.opentelemetry.collector;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.autoconfigure.DevServicesAutoConfiguration;
import io.arconia.dev.services.core.registration.DevServiceCategories;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.opentelemetry.collector.OtelCollectorDevServicesAutoConfiguration.OtelCollectorDevServicesRegistrar;
import io.arconia.opentelemetry.autoconfigure.ConditionalOnOpenTelemetry;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.OtlpConnectionDetails;

/**
 * Auto-configuration for OpenTelemetry Collector Dev Services.
 */
@AutoConfiguration(after = DevServicesAutoConfiguration.class, before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("otel-collector")
@ConditionalOnOpenTelemetry
@EnableConfigurationProperties(OtelCollectorDevServicesProperties.class)
@Import(OtelCollectorDevServicesRegistrar.class)
public final class OtelCollectorDevServicesAutoConfiguration {

    static class OtelCollectorDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(OtelCollectorDevServicesProperties.CONFIG_PREFIX, OtelCollectorDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("otel-collector")
                    .description("OpenTelemetry Collector Dev Service")
                    .category(DevServiceCategories.OPENTELEMETRY)
                    .properties(properties)
                    .container(ArconiaOtelCollectorContainer.class, () -> new ArconiaOtelCollectorContainer(properties))
                    .serviceConnectionName(ArconiaOtelCollectorContainer.COMPATIBLE_IMAGE_NAME)
                    .discovery(OtlpConnectionDetails.class, OtelCollectorDiscoveredConnectionDetails::new)
                    .link("OTLP/gRPC", ArconiaOtelCollectorContainer.OTLP_GRPC_PORT)
                    .link("OTLP/HTTP", ArconiaOtelCollectorContainer.OTLP_HTTP_PORT));
        }

    }

}
