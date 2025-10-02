package io.arconia.dev.services.kafka;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.core.config.DevServicesProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link KafkaDevServicesProperties}.
 */
class KafkaDevServicesPropertiesTests {

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        KafkaDevServicesProperties properties = new KafkaDevServicesProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).contains("apache/kafka-native");
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.DEV_MODE);
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    void shouldUpdateValues() {
        KafkaDevServicesProperties properties = new KafkaDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("apache/kafka-native:latest");
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setShared(DevServicesProperties.Shared.ALWAYS);
        properties.setStartupTimeout(Duration.ofMinutes(5));

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("apache/kafka-native:latest");
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.ALWAYS);
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(5));
    }

}
