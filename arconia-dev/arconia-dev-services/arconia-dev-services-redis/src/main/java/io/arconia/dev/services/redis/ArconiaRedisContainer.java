package io.arconia.dev.services.redis;

import org.testcontainers.utility.DockerImageName;

import io.arconia.testcontainers.redis.RedisContainer;

/**
 * A {@link RedisContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaRedisContainer extends RedisContainer {

    private final RedisDevServicesProperties properties;

    /**
     * Redis RESP protocol port.
     */
    private static final int REDIS_PORT = 6379;

    public ArconiaRedisContainer(DockerImageName dockerImageName, RedisDevServicesProperties properties) {
        super(dockerImageName);
        this.properties = properties;
    }

    @Override
    protected void configure() {
        super.configure();
        if (properties.getPort() > 0) {
            addFixedExposedPort(properties.getPort(), REDIS_PORT);
        }
    }
}
