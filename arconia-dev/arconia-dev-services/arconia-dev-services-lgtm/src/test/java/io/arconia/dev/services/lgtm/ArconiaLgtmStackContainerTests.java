package io.arconia.dev.services.lgtm;

import io.arconia.dev.services.api.config.ResourceMapping;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArconiaLgtmStackContainer}.
 */
class ArconiaLgtmStackContainerTests {

    private static final String SPRING_BOOT_DASHBOARDS_PATH = "/otel-lgtm/spring-boot-dashboards";

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
    void whenSpringBootDashboardIsNotConfigured() {
        var properties = new LgtmDevServicesProperties();
        var container = new ArconiaLgtmStackContainer(properties);

        assertThat(container.getCopyToFileContainerPathMap()).doesNotContainValue(SPRING_BOOT_DASHBOARDS_PATH);
    }

    @Test
    void whenSpringBootDashboardIsConfigured() {
        var properties = springBootDashboardProperties();
        var container = new ArconiaLgtmStackContainer(properties);

        assertThat(container.getCopyToFileContainerPathMap()).containsValue(SPRING_BOOT_DASHBOARDS_PATH + "/spring-boot.json");
    }

    @Test
    void whenSpringBootDashboardIsConfiguredThenProviderIsCorrect() {
        var container = new ArconiaLgtmStackContainer(
                springBootDashboardProperties());

        assertThat(container.springBootDashboardsProvider())
                .contains("apiVersion: 1")
                .contains("name: Spring Boot")
                .contains("type: file")
                .contains("path: " + SPRING_BOOT_DASHBOARDS_PATH)
                .contains("foldersFromFilesStructure: false");
    }

    private LgtmDevServicesProperties springBootDashboardProperties() {
        var properties = new LgtmDevServicesProperties();
        var resource = new ResourceMapping("classpath:grafana/spring-boot.json", SPRING_BOOT_DASHBOARDS_PATH+"/spring-boot.json");
        properties.setResources(List.of(resource));

        return properties;
    }

}
