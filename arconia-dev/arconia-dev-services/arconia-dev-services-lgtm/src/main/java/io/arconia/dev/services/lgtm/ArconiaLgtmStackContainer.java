package io.arconia.dev.services.lgtm;

import java.util.List;

import com.github.dockerjava.api.command.InspectContainerResponse;

import org.testcontainers.grafana.LgtmStackContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.api.registration.DevServiceLinkDefinition;
import io.arconia.dev.services.api.registration.DevServiceLinkProvider;
import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * A {@link LgtmStackContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaLgtmStackContainer extends LgtmStackContainer implements DevServiceLinkProvider {

    private final LgtmDevServicesProperties properties;

    static final String COMPATIBLE_IMAGE_NAME = "grafana/otel-lgtm";

    static final int GRAFANA_PORT = 3000;

    static final int OTLP_GRPC_PORT = 4317;

    static final int OTLP_HTTP_PORT = 4318;

    static final int LOKI_PORT = 3100;

    static final int TEMPO_PORT = 3200;

    static final int PROMETHEUS_PORT = 9090;

    public ArconiaLgtmStackContainer(LgtmDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        this.properties = properties;

        this.withEnv("GF_USERS_DEFAULT_THEME", "system");
        ContainerConfigurer.base(this, properties);
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isFixedPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), GRAFANA_PORT);
        }
        if (ContainerUtils.isFixedPort(properties.getOtlpGrpcPort())) {
            addFixedExposedPort(properties.getOtlpGrpcPort(), OTLP_GRPC_PORT);
        }
        if (ContainerUtils.isFixedPort(properties.getOtlpHttpPort())) {
            addFixedExposedPort(properties.getOtlpHttpPort(), OTLP_HTTP_PORT);
        }
        if (ContainerUtils.isFixedPort(properties.getLokiPort())) {
            addFixedExposedPort(properties.getLokiPort(), LOKI_PORT);
        }
        if (ContainerUtils.isFixedPort(properties.getTempoPort())) {
            addFixedExposedPort(properties.getTempoPort(), TEMPO_PORT);
        }
        if (ContainerUtils.isFixedPort(properties.getPrometheusPort())) {
            addFixedExposedPort(properties.getPrometheusPort(), PROMETHEUS_PORT);
        }
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        // Suppress the superclass's ad-hoc "Access to the Grafana dashboard" log line;
        // Arconia emits a consistent startup message instead.
    }

    @Override
    public List<DevServiceLinkDefinition> devServiceLinkDefinitions() {
        return List.of(
                DevServiceLinkDefinition.builder().id("grafana").label("Grafana").port(GRAFANA_PORT).build(),
                DevServiceLinkDefinition.builder().id("otlp-http").label("OTLP/HTTP").port(OTLP_HTTP_PORT).build(),
                DevServiceLinkDefinition.builder().id("otlp-grpc").label("OTLP/gRPC").port(OTLP_GRPC_PORT).build());
    }

}
