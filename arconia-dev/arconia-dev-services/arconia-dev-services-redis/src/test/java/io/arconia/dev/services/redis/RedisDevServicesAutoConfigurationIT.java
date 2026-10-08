package io.arconia.dev.services.redis;

import org.springframework.boot.data.redis.autoconfigure.DataRedisConnectionDetails;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationIT;
import io.arconia.testcontainers.redis.RedisContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link RedisDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class RedisDevServicesAutoConfigurationIT extends BaseDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = defaultContextRunner(RedisDevServicesAutoConfiguration.class);

    @Override
    protected ApplicationContextRunner getContextRunner() {
        return contextRunner;
    }

    @Override
    protected Class<?> getAutoConfigurationClass() {
        return RedisDevServicesAutoConfiguration.class;
    }

    @Override
    protected Class<? extends GenericContainer<?>> getContainerClass() {
        return RedisContainer.class;
    }

    @Override
    protected String getServiceName() {
        return "redis";
    }

    @Override
    protected Class<?> getConnectionDetailsClass() {
        return DataRedisConnectionDetails.class;
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        var properties = new RedisDevServicesProperties();
        return asDiscoverableContainer(new ArconiaRedisContainer(properties), properties, ownerId);
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> discoveredContainer) {
        DataRedisConnectionDetails connectionDetails = context.getBean(DataRedisConnectionDetails.class);
        assertThat(connectionDetails.getStandalone()).isNotNull();
        assertThat(connectionDetails.getStandalone().getHost()).isEqualTo(discoveredContainer.getHost());
        assertThat(connectionDetails.getStandalone().getPort())
                .isEqualTo(discoveredContainer.getMappedPort(ArconiaRedisContainer.REDIS_PORT));
    }

}
