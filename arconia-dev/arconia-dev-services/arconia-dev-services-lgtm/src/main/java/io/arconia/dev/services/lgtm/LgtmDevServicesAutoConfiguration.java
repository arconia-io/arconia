package io.arconia.dev.services.lgtm;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.testcontainers.grafana.LgtmStackContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.api.config.ResourceMapping;
import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.autoconfigure.DevServicesAutoConfiguration;
import io.arconia.dev.services.core.registration.DevServiceCategories;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.lgtm.LgtmDevServicesAutoConfiguration.LgtmDevServicesRegistrar;
import io.arconia.opentelemetry.autoconfigure.ConditionalOnOpenTelemetry;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.OtlpConnectionDetails;

/**
 * Auto-configuration for Grafana LGTM Dev Services.
 */
@AutoConfiguration(after = DevServicesAutoConfiguration.class, before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnOpenTelemetry
@ConditionalOnDevServicesEnabled("lgtm")
@EnableConfigurationProperties(LgtmDevServicesProperties.class)
@Import(LgtmDevServicesRegistrar.class)
public final class LgtmDevServicesAutoConfiguration {

    static final String COMPATIBLE_IMAGE_NAME = "grafana/otel-lgtm";

    static final int GRAFANA_PORT = 3000;

    static final int OTLP_GRPC_PORT = 4317;

    static final int OTLP_HTTP_PORT = 4318;

    static final int LOKI_PORT = 3100;

    static final int TEMPO_PORT = 3200;

    static final int PROMETHEUS_PORT = 9090;

    static final String SPRING_BOOT_DASHBOARDS_PATH = "/otel-lgtm/spring-boot-dashboards";

    static final String GRAFANA_DASHBOARD_PROVISIONING_PATH = "/otel-lgtm/grafana/conf/provisioning/dashboards";

    static final String SPRING_BOOT_DASHBOARD_PROVIDER_FILE = "spring-boot-dashboards.yaml";

    static LgtmStackContainer createContainer(LgtmDevServicesProperties properties) {
        var container = new LgtmStackContainer(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        container.withEnv("GF_USERS_DEFAULT_THEME", "system");
        configureSpringBootDashboards(container, properties);
        return container;
    }

    /**
     * Registers a Grafana dashboard provider for the Spring Boot dashboards
     * when at least one resource is mapped under the dedicated container path.
     */
    private static void configureSpringBootDashboards(LgtmStackContainer container, LgtmDevServicesProperties properties) {
        if (!hasSpringBootDashboardResources(properties)) {
            return;
        }
        container.withCopyToContainer(Transferable.of(springBootDashboardsProvider().getBytes(StandardCharsets.UTF_8)),
                GRAFANA_DASHBOARD_PROVISIONING_PATH + "/" + SPRING_BOOT_DASHBOARD_PROVIDER_FILE);
    }

    static boolean hasSpringBootDashboardResources(LgtmDevServicesProperties properties) {
        return properties.getResources()
                .stream()
                .map(ResourceMapping::containerPath)
                .anyMatch(LgtmDevServicesAutoConfiguration::isSpringBootDashboardPath);
    }

    static String springBootDashboardsProvider() {
        return """
                apiVersion: 1

                providers:
                  - name: Spring Boot
                    type: file
                    options:
                      path: %s
                      foldersFromFilesStructure: false
                """.formatted(SPRING_BOOT_DASHBOARDS_PATH);
    }

    private static boolean isSpringBootDashboardPath(String containerPath) {
        return containerPath.equals(SPRING_BOOT_DASHBOARDS_PATH)
                || containerPath.startsWith(SPRING_BOOT_DASHBOARDS_PATH + "/");
    }

    static class LgtmDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(LgtmDevServicesProperties.CONFIG_PREFIX, LgtmDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("lgtm")
                    .description("Grafana LGTM Dev Service")
                    .category(DevServiceCategories.OPENTELEMETRY)
                    .properties(properties)
                    .container(LgtmStackContainer.class, () -> createContainer(properties))
                    .port(GRAFANA_PORT)
                    .additionalPort(OTLP_GRPC_PORT, properties.getOtlpGrpcPort())
                    .additionalPort(OTLP_HTTP_PORT, properties.getOtlpHttpPort())
                    .additionalPort(LOKI_PORT, properties.getLokiPort())
                    .additionalPort(TEMPO_PORT, properties.getTempoPort())
                    .additionalPort(PROMETHEUS_PORT, properties.getPrometheusPort())
                    .discovery(OtlpConnectionDetails.class, LgtmDiscoveredConnectionDetails::new)
                    .link("Grafana", GRAFANA_PORT)
                    .link("OTLP/HTTP", OTLP_HTTP_PORT)
                    .link("OTLP/gRPC", OTLP_GRPC_PORT));
        }

    }

}
