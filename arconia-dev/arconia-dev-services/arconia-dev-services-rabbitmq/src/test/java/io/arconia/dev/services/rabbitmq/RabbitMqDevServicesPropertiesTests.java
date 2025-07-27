package io.arconia.dev.services.rabbitmq;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.core.config.DevServicesProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RabbitMqDevServicesProperties}.
 */
class RabbitMqDevServicesPropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(RabbitMqDevServicesProperties.CONFIG_PREFIX)
                .isEqualTo("arconia.dev.services.rabbitmq");
    }

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        RabbitMqDevServicesProperties properties = new RabbitMqDevServicesProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).contains("rabbitmq");
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.DEV_MODE);
    }

    @Test
    void shouldUpdateValues() {
        RabbitMqDevServicesProperties properties = new RabbitMqDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("rabbitmq:latest");
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setShared(DevServicesProperties.Shared.ALWAYS);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("rabbitmq:latest");
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.ALWAYS);
    }

}
