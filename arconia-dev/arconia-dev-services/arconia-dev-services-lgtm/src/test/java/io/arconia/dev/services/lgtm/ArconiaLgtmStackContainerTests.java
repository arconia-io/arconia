package io.arconia.dev.services.lgtm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArconiaLgtmStackContainer}.
 */
class ArconiaLgtmStackContainerTests {

    @Test
    void whenExposedPortsAreNotConfigured() {
        var container = new ArconiaLgtmStackContainer(new LgtmDevServicesProperties());
        container.configure();
        assertThat(container.getPortBindings()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void whenExposedPortsAreConfigured() {
        var properties = new LgtmDevServicesProperties();
        properties.setPort(1234);
        properties.setOtlpGrpcPort(5678);
        properties.setOtlpHttpPort(9067);
        properties.setLokiPort(9001);
        properties.setTempoPort(9002);
        properties.setPrometheusPort(9003);

        var container = new ArconiaLgtmStackContainer(properties);
        container.configure();

        var portBindings = container.getPortBindings();
        assertThat(portBindings).isNotNull();
        assertThat(portBindings)
                .anyMatch(binding -> binding.startsWith(
                        properties.getPort() + ":" + ArconiaLgtmStackContainer.GRAFANA_PORT))
                .anyMatch(binding -> binding.startsWith(
                        properties.getOtlpGrpcPort() + ":" + ArconiaLgtmStackContainer.OTLP_GRPC_PORT))
                .anyMatch(binding -> binding.startsWith(
                        properties.getOtlpHttpPort() + ":" + ArconiaLgtmStackContainer.OTLP_HTTP_PORT))
                .anyMatch(binding -> binding.startsWith(
                        properties.getLokiPort() + ":" + ArconiaLgtmStackContainer.LOKI_PORT))
                .anyMatch(binding -> binding.startsWith(
                        properties.getTempoPort() + ":" + ArconiaLgtmStackContainer.TEMPO_PORT))
                .anyMatch(binding -> binding.startsWith(
                        properties.getPrometheusPort() + ":" + ArconiaLgtmStackContainer.PROMETHEUS_PORT));
    }

    @Test
    void grafanaThemeFollowsTheSystem() {
        var container = new ArconiaLgtmStackContainer(new LgtmDevServicesProperties());
        assertThat(container.getEnvMap()).containsEntry("GF_USERS_DEFAULT_THEME", "system");
    }

}
