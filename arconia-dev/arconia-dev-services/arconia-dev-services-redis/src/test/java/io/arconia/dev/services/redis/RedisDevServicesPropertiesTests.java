package io.arconia.dev.services.redis;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.api.config.ResourceMapping;
import io.arconia.testcontainers.redis.RedisContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RedisDevServicesProperties}.
 */
class RedisDevServicesPropertiesTests {

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        RedisDevServicesProperties properties = new RedisDevServicesProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).containsIgnoringCase("redis");
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.getNetworkAliases()).isEmpty();
        assertThat(properties.getPort()).isEqualTo(0);
        assertThat(properties.getResources()).isEmpty();
        assertThat(properties.isShared()).isFalse();
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void shouldUpdateValues() {
        RedisDevServicesProperties properties = new RedisDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("redis:latest");
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setNetworkAliases(List.of("network1", "network2"));
        properties.setPort(RedisContainer.REDIS_PORT);
        properties.setResources(List.of(new ResourceMapping("test-resource.txt", "/tmp/test-resource.txt")));
        properties.setShared(true);
        properties.setStartupTimeout(Duration.ofMinutes(1));

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("redis:latest");
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.getNetworkAliases()).containsExactly("network1", "network2");
        assertThat(properties.getPort()).isEqualTo(RedisContainer.REDIS_PORT);
        assertThat(properties.getResources()).hasSize(1);
        assertThat(properties.getResources().getFirst().getSourcePath()).isEqualTo("test-resource.txt");
        assertThat(properties.getResources().getFirst().getContainerPath()).isEqualTo("/tmp/test-resource.txt");
        assertThat(properties.isShared()).isTrue();
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(1));
    }

}
