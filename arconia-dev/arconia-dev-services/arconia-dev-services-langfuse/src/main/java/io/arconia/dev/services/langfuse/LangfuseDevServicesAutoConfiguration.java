package io.arconia.dev.services.langfuse;

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
import io.arconia.dev.services.langfuse.LangfuseDevServicesAutoConfiguration.LangfuseDevServicesRegistrar;
import io.arconia.opentelemetry.autoconfigure.ConditionalOnOpenTelemetry;
import io.arconia.opentelemetry.autoconfigure.exporter.OpenTelemetryExporterAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.OpenTelemetryLoggingExporterProperties;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.OpenTelemetryMetricsExporterProperties;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingConnectionDetails;
import io.arconia.testcontainers.langfuse.LangfuseContainer;

/**
 * Auto-configuration for Langfuse Dev Services.
 */
@AutoConfiguration(before = { OpenTelemetryExporterAutoConfiguration.class, ServiceConnectionAutoConfiguration.class })
@ConditionalOnDevServicesEnabled("langfuse")
@ConditionalOnOpenTelemetry
@EnableConfigurationProperties(LangfuseDevServicesProperties.class)
@Import(LangfuseDevServicesRegistrar.class)
public final class LangfuseDevServicesAutoConfiguration {

    static final String COMPATIBLE_IMAGE_NAME = "langfuse/langfuse";

    static LangfuseContainer createContainer(LangfuseDevServicesProperties properties) {
        return new LangfuseContainer(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                .withWorkerImage(properties.getWorkerImageName())
                .withPostgresImage(properties.getPostgresImageName())
                .withClickHouseImage(properties.getClickhouseImageName())
                .withRedisImage(properties.getRedisImageName())
                .withFlociImage(properties.getFlociImageName())
                .withEnv("LANGFUSE_INIT_PROJECT_PUBLIC_KEY", properties.getPublicKey())
                .withEnv("LANGFUSE_INIT_PROJECT_SECRET_KEY", properties.getSecretKey());
    }

    static class LangfuseDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(LangfuseDevServicesProperties.CONFIG_PREFIX, LangfuseDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("langfuse")
                    .description("Langfuse Dev Service")
                    .category(DevServiceCategories.OPENTELEMETRY)
                    .properties(properties)
                    .container(LangfuseContainer.class, () -> createContainer(properties))
                    .port(LangfuseContainer.HTTP_PORT)
                    .discovery(OtlpTracingConnectionDetails.class,
                            container -> new LangfuseDiscoveredConnectionDetails(container, properties))
                    .link("Langfuse UI", LangfuseContainer.HTTP_PORT));

            // Langfuse accepts traces only. Logs and metrics are not exported while the dev service is active.
            addDynamicProperty(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".type", () -> "none");
            addDynamicProperty(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".type", () -> "none");
        }

    }

}
