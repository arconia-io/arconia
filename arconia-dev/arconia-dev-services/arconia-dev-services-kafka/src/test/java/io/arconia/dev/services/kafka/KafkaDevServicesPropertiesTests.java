package io.arconia.dev.services.kafka;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.api.config.ResourceMapping;

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
        assertThat(properties.getNetworkAliases()).isEmpty();
        assertThat(properties.getPort()).isEqualTo(0);
        assertThat(properties.getResources()).isEmpty();
        assertThat(properties.isShared()).isTrue();
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void shouldUpdateValues() {
        KafkaDevServicesProperties properties = new KafkaDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("apache/kafka-native:latest");
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setNetworkAliases(List.of("network1", "network2"));
        properties.setPort(ArconiaKafkaContainer.KAFKA_PORT);
        properties.setResources(List.of(new ResourceMapping("test-resource.txt", "/tmp/test-resource.txt")));
        properties.setShared(false);
        properties.setStartupTimeout(Duration.ofMinutes(1));

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("apache/kafka-native:latest");
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.getNetworkAliases()).containsExactly("network1", "network2");
        assertThat(properties.getPort()).isEqualTo(ArconiaKafkaContainer.KAFKA_PORT);
        assertThat(properties.getResources()).hasSize(1);
        assertThat(properties.getResources().getFirst().getSourcePath()).isEqualTo("test-resource.txt");
        assertThat(properties.getResources().getFirst().getContainerPath()).isEqualTo("/tmp/test-resource.txt");
        assertThat(properties.isShared()).isFalse();
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(1));
    }

}
