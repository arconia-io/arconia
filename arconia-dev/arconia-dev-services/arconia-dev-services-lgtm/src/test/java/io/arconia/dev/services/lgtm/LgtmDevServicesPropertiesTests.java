package io.arconia.dev.services.lgtm;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LgtmDevServicesProperties}.
 */
class LgtmDevServicesPropertiesTests {

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        LgtmDevServicesProperties properties = new LgtmDevServicesProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).contains("grafana/otel-lgtm");
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.getNetworkAliases()).isEmpty();
        assertThat(properties.getPort()).isEqualTo(0);
        assertThat(properties.isShared()).isTrue();
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(2));

        assertThat(properties.getOtlpGrpcPort()).isEqualTo(0);
        assertThat(properties.getOtlpHttpPort()).isEqualTo(0);
    }

    @Test
    void shouldUpdateValues() {
        LgtmDevServicesProperties properties = new LgtmDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("grafana/otel-lgtm:latest");
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setNetworkAliases(List.of("network1", "network2"));
        properties.setPort(ArconiaLgtmStackContainer.GRAFANA_PORT);
        properties.setShared(false);
        properties.setStartupTimeout(Duration.ofMinutes(1));

        properties.setOtlpGrpcPort(ArconiaLgtmStackContainer.OTLP_GRPC_PORT);
        properties.setOtlpHttpPort(ArconiaLgtmStackContainer.OTLP_HTTP_PORT);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("grafana/otel-lgtm:latest");
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.getNetworkAliases()).containsExactly("network1", "network2");
        assertThat(properties.getPort()).isEqualTo(ArconiaLgtmStackContainer.GRAFANA_PORT);
        assertThat(properties.isShared()).isFalse();
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(1));

        assertThat(properties.getOtlpGrpcPort()).isEqualTo(ArconiaLgtmStackContainer.OTLP_GRPC_PORT);
        assertThat(properties.getOtlpHttpPort()).isEqualTo(ArconiaLgtmStackContainer.OTLP_HTTP_PORT);
    }

}
