package io.arconia.dev.services.lgtm;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.tests.BaseDevServiceContainerTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArconiaLgtmStackContainer}.
 */
class ArconiaLgtmStackContainerTests extends BaseDevServiceContainerTests<LgtmDevServicesProperties, ArconiaLgtmStackContainer> {

    @Override
    protected LgtmDevServicesProperties createProperties() {
        return new LgtmDevServicesProperties();
    }

    @Override
    protected ArconiaLgtmStackContainer createContainer(LgtmDevServicesProperties properties) {
        return new ArconiaLgtmStackContainer(properties);
    }

    @Override
    protected Map<Integer, Integer> configureFixedPorts(LgtmDevServicesProperties properties) {
        properties.setPort(1234);
        properties.setOtlpGrpcPort(5678);
        properties.setOtlpHttpPort(9067);
        properties.setLokiPort(9001);
        properties.setTempoPort(9002);
        properties.setPrometheusPort(9003);
        return Map.of(
                1234, ArconiaLgtmStackContainer.GRAFANA_PORT,
                5678, ArconiaLgtmStackContainer.OTLP_GRPC_PORT,
                9067, ArconiaLgtmStackContainer.OTLP_HTTP_PORT,
                9001, ArconiaLgtmStackContainer.LOKI_PORT,
                9002, ArconiaLgtmStackContainer.TEMPO_PORT,
                9003, ArconiaLgtmStackContainer.PROMETHEUS_PORT);
    }

    @Test
    void grafanaThemeFollowsTheSystem() {
        var container = new ArconiaLgtmStackContainer(new LgtmDevServicesProperties());

        assertThat(container.getEnvMap()).containsEntry("GF_USERS_DEFAULT_THEME", "system");
    }

}
