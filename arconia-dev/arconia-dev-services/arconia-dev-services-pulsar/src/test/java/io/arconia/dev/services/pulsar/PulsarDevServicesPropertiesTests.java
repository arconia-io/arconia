package io.arconia.dev.services.pulsar;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.core.config.DevServicesProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PulsarDevServicesProperties}.
 */
class PulsarDevServicesPropertiesTests {

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        PulsarDevServicesProperties properties = new PulsarDevServicesProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).contains("apachepulsar/pulsar");
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.DEV_MODE);
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    void shouldUpdateValues() {
        PulsarDevServicesProperties properties = new PulsarDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("apachepulsar/pulsar:latest");
        properties.setPort(ArconiaPulsarContainer.PULSAR_PORT);
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setShared(DevServicesProperties.Shared.ALWAYS);
        properties.setStartupTimeout(Duration.ofMinutes(5));

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("apachepulsar/pulsar:latest");
        assertThat(properties.getPort()).isEqualTo(ArconiaPulsarContainer.PULSAR_PORT);
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.ALWAYS);
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(5));
    }

}
