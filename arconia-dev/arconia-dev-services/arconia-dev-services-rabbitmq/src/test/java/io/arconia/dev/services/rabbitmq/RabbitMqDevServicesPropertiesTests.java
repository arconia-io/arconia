package io.arconia.dev.services.rabbitmq;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.api.config.ResourceMapping;

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
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.getNetworkAliases()).isEmpty();
        assertThat(properties.getPort()).isEqualTo(0);
        assertThat(properties.getResources()).isEmpty();
        assertThat(properties.isShared()).isTrue();
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(properties.getManagementConsolePort()).isEqualTo(0);
    }

    @Test
    void shouldUpdateValues() {
        RabbitMqDevServicesProperties properties = new RabbitMqDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("rabbitmq:latest");
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setNetworkAliases(List.of("network1", "network2"));
        properties.setPort(ArconiaRabbitMqContainer.AMQP_PORT);
        properties.setResources(List.of(new ResourceMapping("test-resource.txt", "/tmp/test-resource.txt")));
        properties.setShared(false);
        properties.setStartupTimeout(Duration.ofMinutes(1));
        properties.setManagementConsolePort(ArconiaRabbitMqContainer.HTTP_PORT);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("rabbitmq:latest");
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.getNetworkAliases()).containsExactly("network1", "network2");
        assertThat(properties.getPort()).isEqualTo(ArconiaRabbitMqContainer.AMQP_PORT);
        assertThat(properties.getResources()).hasSize(1);
        assertThat(properties.getResources().getFirst().getSourcePath()).isEqualTo("test-resource.txt");
        assertThat(properties.getResources().getFirst().getContainerPath()).isEqualTo("/tmp/test-resource.txt");
        assertThat(properties.isShared()).isFalse();
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(1));
        assertThat(properties.getManagementConsolePort()).isEqualTo(ArconiaRabbitMqContainer.HTTP_PORT);
    }

}
