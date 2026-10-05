package io.arconia.dev.services.redis;

import org.springframework.boot.data.redis.autoconfigure.DataRedisConnectionDetails;

import io.arconia.dev.services.core.registration.DiscoveredContainer;

/**
 * {@link DataRedisConnectionDetails} for connecting to a Redis dev service
 * running in a container discovered from another application.
 */
final class RedisDiscoveredConnectionDetails implements DataRedisConnectionDetails {

    private final Standalone standalone;

    RedisDiscoveredConnectionDetails(DiscoveredContainer container) {
        this.standalone = Standalone.of(container.host(), container.mappedPort(ArconiaRedisContainer.REDIS_PORT));
    }

    @Override
    public Standalone getStandalone() {
        return standalone;
    }

}
