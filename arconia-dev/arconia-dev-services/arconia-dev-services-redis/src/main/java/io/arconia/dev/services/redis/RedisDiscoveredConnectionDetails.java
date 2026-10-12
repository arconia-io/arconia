package io.arconia.dev.services.redis;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.data.redis.autoconfigure.DataRedisConnectionDetails;
import org.springframework.boot.ssl.SslBundle;

import io.arconia.dev.services.core.registration.DiscoveredContainer;
import io.arconia.testcontainers.redis.RedisContainer;

/**
 * {@link DataRedisConnectionDetails} for connecting to a Redis dev service
 * running in a container discovered from another application.
 */
final class RedisDiscoveredConnectionDetails implements DataRedisConnectionDetails {

    private final Standalone standalone;

    @Nullable
    private final SslBundle sslBundle;

    RedisDiscoveredConnectionDetails(DiscoveredContainer container) {
        this.standalone = Standalone.of(container.host(), container.mappedPort(RedisContainer.REDIS_PORT));
        this.sslBundle = container.sslBundle();
    }

    @Override
    public Standalone getStandalone() {
        return standalone;
    }

    @Override
    @Nullable
    public SslBundle getSslBundle() {
        return sslBundle;
    }

}
