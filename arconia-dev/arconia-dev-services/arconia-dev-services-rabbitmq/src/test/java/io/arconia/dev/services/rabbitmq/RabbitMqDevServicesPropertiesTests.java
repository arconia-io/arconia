package io.arconia.dev.services.rabbitmq;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.core.config.DevServicesProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RabbitMqDevServicesProperties}.
 */
class RabbitMqDevServicesPropertiesTests {

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        RabbitMqDevServicesProperties properties = new RabbitMqDevServicesProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).contains("rabbitmq");
        assertThat(properties.getPort()).isEqualTo(0);
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.DEV_MODE);
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    void shouldUpdateValues() {
        RabbitMqDevServicesProperties properties = new RabbitMqDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("rabbitmq:latest");
        properties.setPort(ArconiaRabbitMqContainer.RABBITMQ_PORT);
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setShared(DevServicesProperties.Shared.ALWAYS);
        properties.setStartupTimeout(Duration.ofMinutes(5));

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("rabbitmq:latest");
        assertThat(properties.getPort()).isEqualTo(ArconiaRabbitMqContainer.RABBITMQ_PORT);
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.ALWAYS);
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(5));
    }

}
