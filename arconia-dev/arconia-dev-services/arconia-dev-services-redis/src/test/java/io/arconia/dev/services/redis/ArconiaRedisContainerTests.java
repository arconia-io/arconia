package io.arconia.dev.services.redis;

import org.junit.jupiter.api.Test;

import io.arconia.testcontainers.redis.RedisContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArconiaRedisContainer}.
 */
class ArconiaRedisContainerTests {

    @Test
    void whenExposedPortsAreNotConfigured() {
        var container = new ArconiaRedisContainer(new RedisDevServicesProperties());
        container.configure();
        assertThat(container.getPortBindings()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void whenExposedPortsAreConfigured() {
        var properties = new RedisDevServicesProperties();
        properties.setPort(1234);

        var container = new ArconiaRedisContainer(properties);
        container.configure();

        var portBindings = container.getPortBindings();
        assertThat(portBindings).isNotNull();
        assertThat(portBindings)
                .anyMatch(binding -> binding.startsWith(
                        properties.getPort() + ":" + RedisContainer.REDIS_PORT));
    }

}
