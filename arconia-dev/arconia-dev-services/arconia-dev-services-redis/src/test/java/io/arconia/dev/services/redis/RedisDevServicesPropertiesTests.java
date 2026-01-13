package io.arconia.dev.services.redis;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.core.config.DevServicesProperties;

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
        assertThat(properties.getPort()).isEqualTo(0);
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.NEVER);
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    void shouldUpdateValues() {
        RedisDevServicesProperties properties = new RedisDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("redis:latest");
        properties.setPort(6379);
        properties.setEnvironment(Map.of("REDIS_PASSWORD", "redis"));
        properties.setShared(DevServicesProperties.Shared.ALWAYS);
        properties.setStartupTimeout(Duration.ofMinutes(5));

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("redis:latest");
        assertThat(properties.getPort()).isEqualTo(6379);
        assertThat(properties.getEnvironment()).containsEntry("REDIS_PASSWORD", "redis");
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.ALWAYS);
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(5));
    }

}
