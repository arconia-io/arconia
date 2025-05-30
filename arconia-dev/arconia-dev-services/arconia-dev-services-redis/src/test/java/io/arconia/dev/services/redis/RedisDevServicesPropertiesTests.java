package io.arconia.dev.services.redis;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.core.config.DevServicesProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RedisDevServicesProperties}.
 */
class RedisDevServicesPropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(RedisDevServicesProperties.CONFIG_PREFIX)
                .isEqualTo("arconia.dev.services.redis");
    }

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        RedisDevServicesProperties properties = new RedisDevServicesProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).containsIgnoringCase("redis");
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.NEVER);
    }

    @Test
    void shouldUpdateValues() {
        RedisDevServicesProperties properties = new RedisDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("redis:latest");
        properties.setEnvironment(Map.of("REDIS_PASSWORD", "redis"));
        properties.setShared(DevServicesProperties.Shared.ALWAYS);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("redis:latest");
        assertThat(properties.getEnvironment()).containsEntry("REDIS_PASSWORD", "redis");
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.ALWAYS);
    }

}
