package io.arconia.dev.services.rabbitmq;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RabbitMQDevServicesProperties}.
 */
class RabbitMQDevServicesPropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(RabbitMQDevServicesProperties.CONFIG_PREFIX)
                .isEqualTo("arconia.dev.services.rabbitmq");
    }

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        RabbitMQDevServicesProperties properties = new RabbitMQDevServicesProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).contains("rabbitmq");
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.isReusable()).isFalse();
    }

    @Test
    void shouldUpdateValues() {
        RabbitMQDevServicesProperties properties = new RabbitMQDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("rabbitmq:latest");
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setReusable(false);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("rabbitmq:latest");
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.isReusable()).isFalse();
    }

}
