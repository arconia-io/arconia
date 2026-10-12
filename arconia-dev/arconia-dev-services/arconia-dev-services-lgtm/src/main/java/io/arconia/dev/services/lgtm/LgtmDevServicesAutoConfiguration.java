package io.arconia.dev.services.lgtm;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.testcontainers.grafana.LgtmStackContainer;
import org.testcontainers.utility.DockerImageName;

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

    /**
     * Path of the Tempo MCP server, which AI tools use to query traces.
     */
    static final String TEMPO_MCP_PATH = "/api/mcp";

    /**
     * Tempo accepts extra command line arguments from this environment variable. The MCP server
     * is off by default in the image and enabled here for AI tools to query traces.
     */
    static final String TEMPO_EXTRA_ARGS_ENV = "TEMPO_EXTRA_ARGS";

    static final String TEMPO_MCP_SERVER_ARG = "--query-frontend.mcp-server.enabled=true";

    static LgtmStackContainer createContainer(LgtmDevServicesProperties properties, LgtmDashboards dashboards) {
        var container = new LgtmStackContainer(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        container.withEnv("GF_USERS_DEFAULT_THEME", "system");
        container.withEnv(TEMPO_EXTRA_ARGS_ENV, TEMPO_MCP_SERVER_ARG);
        dashboards.applyTo(container);
        return container;
    }

    static class LgtmDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(LgtmDevServicesProperties.CONFIG_PREFIX, LgtmDevServicesProperties.class);
            var dashboards = LgtmDashboards.collect(environment, getResourceLoader(), properties.getDashboards());

            registry.registerDevService(service -> service
                    .name("lgtm")
                    .description("Grafana LGTM Dev Service")
                    .category(DevServiceCategories.OPENTELEMETRY)
                    .properties(properties)
                    .container(LgtmStackContainer.class, () -> createContainer(properties, dashboards))
                    .port(GRAFANA_PORT)
                    .additionalPort(OTLP_GRPC_PORT, properties.getOtlpGrpcPort())
                    .additionalPort(OTLP_HTTP_PORT, properties.getOtlpHttpPort())
                    .additionalPort(LOKI_PORT, properties.getLokiPort())
                    .additionalPort(TEMPO_PORT, properties.getTempoPort())
                    .additionalPort(PROMETHEUS_PORT, properties.getPrometheusPort())
                    .discovery(OtlpConnectionDetails.class, LgtmDiscoveredConnectionDetails::new)
                    .link("Grafana", GRAFANA_PORT)
                    .link("OTLP/HTTP", OTLP_HTTP_PORT)
                    .link("OTLP/gRPC", OTLP_GRPC_PORT)
                    .link("Prometheus", PROMETHEUS_PORT)
                    .link("Tempo MCP", TEMPO_PORT, TEMPO_MCP_PATH));
        }

    }

}
