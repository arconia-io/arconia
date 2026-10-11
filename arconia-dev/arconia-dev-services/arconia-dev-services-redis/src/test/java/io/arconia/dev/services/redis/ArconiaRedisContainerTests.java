package io.arconia.dev.services.redis;

import java.util.Map;

import io.arconia.dev.services.tests.BaseDevServiceContainerTests;
import io.arconia.testcontainers.redis.RedisContainer;

/**
 * Unit tests for {@link ArconiaRedisContainer}.
 */
class ArconiaRedisContainerTests extends BaseDevServiceContainerTests<RedisDevServicesProperties, ArconiaRedisContainer> {

    @Override
    protected RedisDevServicesProperties createProperties() {
        return new RedisDevServicesProperties();
    }

    @Override
    protected ArconiaRedisContainer createContainer(RedisDevServicesProperties properties) {
        return new ArconiaRedisContainer(properties);
    }

    @Override
    protected Map<Integer, Integer> configureFixedPorts(RedisDevServicesProperties properties) {
        properties.setPort(1234);
        return Map.of(1234, RedisContainer.REDIS_PORT);
    }

}
