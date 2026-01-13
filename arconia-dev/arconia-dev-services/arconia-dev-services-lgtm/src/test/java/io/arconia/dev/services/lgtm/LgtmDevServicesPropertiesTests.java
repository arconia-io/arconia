package io.arconia.dev.services.lgtm;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.core.config.DevServicesProperties;

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
        assertThat(properties.getPort()).isEqualTo(0);
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.DEV_MODE);
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    void shouldUpdateValues() {
        LgtmDevServicesProperties properties = new LgtmDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("grafana/otel-lgtm:latest");
        properties.setPort(ArconiaLgtmStackContainer.GRAFANA_PORT);
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setShared(DevServicesProperties.Shared.ALWAYS);
        properties.setStartupTimeout(Duration.ofMinutes(5));

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("grafana/otel-lgtm:latest");
        assertThat(properties.getPort()).isEqualTo(ArconiaLgtmStackContainer.GRAFANA_PORT);
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.ALWAYS);
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(5));
    }

}
