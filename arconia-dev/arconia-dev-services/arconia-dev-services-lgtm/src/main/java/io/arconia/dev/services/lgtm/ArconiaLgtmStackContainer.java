package io.arconia.dev.services.lgtm;

import org.testcontainers.grafana.LgtmStackContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * A {@link LgtmStackContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaLgtmStackContainer extends LgtmStackContainer {

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

}
