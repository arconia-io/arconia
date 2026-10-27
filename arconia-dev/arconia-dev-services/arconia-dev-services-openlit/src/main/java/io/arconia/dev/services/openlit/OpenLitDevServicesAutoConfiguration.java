package io.arconia.dev.services.openlit;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.registration.DevServiceCategories;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.openlit.OpenLitDevServicesAutoConfiguration.OpenLitDevServicesRegistrar;
import io.arconia.opentelemetry.autoconfigure.ConditionalOnOpenTelemetry;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.OtlpConnectionDetails;
import io.arconia.testcontainers.openlit.OpenLitContainer;

/**
 * Auto-configuration for OpenLIT Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnOpenTelemetry
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
