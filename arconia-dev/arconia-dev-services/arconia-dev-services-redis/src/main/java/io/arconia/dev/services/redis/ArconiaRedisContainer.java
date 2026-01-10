package io.arconia.dev.services.redis;

import org.testcontainers.utility.DockerImageName;

import io.arconia.testcontainers.redis.RedisContainer;

/**
 * A {@link RedisContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaRedisContainer extends RedisContainer {

    public ArconiaRedisContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
    }

}
