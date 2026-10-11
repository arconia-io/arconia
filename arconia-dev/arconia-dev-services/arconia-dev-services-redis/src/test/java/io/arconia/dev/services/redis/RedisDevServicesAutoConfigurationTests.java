package io.arconia.dev.services.redis;

import org.springframework.boot.data.redis.autoconfigure.DataRedisConnectionDetails;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.testcontainers.containers.GenericContainer;

import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationTests;
import io.arconia.testcontainers.redis.RedisContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RedisDevServicesAutoConfiguration}.
 */
class RedisDevServicesAutoConfigurationTests extends BaseDevServicesAutoConfigurationTests {

    RedisDevServicesAutoConfigurationTests() {
        super(RedisDevServicesAutoConfiguration.class, "redis", RedisContainer.class, DataRedisConnectionDetails.class);
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        var properties = new RedisDevServicesProperties();
        return asDiscoverableContainer(RedisDevServicesAutoConfiguration.createContainer(properties), properties, ownerId);
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> discoveredContainer) {
        DataRedisConnectionDetails connectionDetails = context.getBean(DataRedisConnectionDetails.class);
        assertThat(connectionDetails.getStandalone()).isNotNull();
        assertThat(connectionDetails.getStandalone().getHost()).isEqualTo(discoveredContainer.getHost());
        assertThat(connectionDetails.getStandalone().getPort())
                .isEqualTo(discoveredContainer.getMappedPort(RedisContainer.REDIS_PORT));
    }

}
