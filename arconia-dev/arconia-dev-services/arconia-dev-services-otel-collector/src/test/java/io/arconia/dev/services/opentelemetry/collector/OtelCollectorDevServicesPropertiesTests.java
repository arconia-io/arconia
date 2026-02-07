package io.arconia.dev.services.opentelemetry.collector;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.api.config.ResourceMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OtelCollectorDevServicesProperties}.
 */
class OtelCollectorDevServicesPropertiesTests {

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        OtelCollectorDevServicesProperties properties = new OtelCollectorDevServicesProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).contains("otel/opentelemetry-collector-contrib");
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.getNetworkAliases()).isEmpty();
        assertThat(properties.getPort()).isEqualTo(0);
        assertThat(properties.getResources()).isEmpty();
        assertThat(properties.isShared()).isTrue();
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(properties.getOtlpGrpcPort()).isEqualTo(0);
    }

    @Test
    void shouldUpdateValues() {
        OtelCollectorDevServicesProperties properties = new OtelCollectorDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("otel/opentelemetry-collector-contrib:latest");
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setNetworkAliases(List.of("network1", "network2"));
        properties.setPort(ArconiaOtelCollectorContainer.OTLP_HTTP_PORT);
        properties.setResources(List.of(new ResourceMapping("test-resource.txt", "/tmp/test-resource.txt")));
        properties.setShared(false);
        properties.setStartupTimeout(Duration.ofMinutes(1));
        properties.setOtlpGrpcPort(ArconiaOtelCollectorContainer.OTLP_GRPC_PORT);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("otel/opentelemetry-collector-contrib:latest");
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.getNetworkAliases()).containsExactly("network1", "network2");
        assertThat(properties.getPort()).isEqualTo(ArconiaOtelCollectorContainer.OTLP_HTTP_PORT);
        assertThat(properties.getResources()).hasSize(1);
        assertThat(properties.getResources().getFirst().getSourcePath()).isEqualTo("test-resource.txt");
        assertThat(properties.getResources().getFirst().getContainerPath()).isEqualTo("/tmp/test-resource.txt");
        assertThat(properties.isShared()).isFalse();
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(1));
        assertThat(properties.getOtlpGrpcPort()).isEqualTo(ArconiaOtelCollectorContainer.OTLP_GRPC_PORT);
    }

}
