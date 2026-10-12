package io.arconia.dev.services.redis;

import org.junit.jupiter.api.Test;
import org.springframework.boot.data.redis.autoconfigure.DataRedisConnectionDetails;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.testcontainers.containers.GenericContainer;

import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationTests;
import io.arconia.dev.services.tests.DevServiceContainers;
import io.arconia.dev.services.tests.SslTestCertificates;
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

    @Test
    void sslApplied() {
        getContextRunner()
                .withPropertyValues(SslTestCertificates.pemProperties(getServiceName()))
                .run(context -> {
                    var container = (RedisContainer) context.getBean(getContainerClass());
                    assertThat(container.getCommandParts()).containsSequence("--tls-port", "6379").containsSequence("--port", "0")
                            .containsSequence("--tls-auth-clients", "no");
                    assertThat(DevServiceContainers.copiedFiles(container)).containsValues(
                            RedisDevServicesAutoConfiguration.SSL_DIRECTORY + "server.crt",
                            RedisDevServicesAutoConfiguration.SSL_DIRECTORY + "server.key",
                            RedisDevServicesAutoConfiguration.SSL_DIRECTORY + "ca.crt");
                });
    }

}
