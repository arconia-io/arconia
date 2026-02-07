package io.arconia.dev.services.redis;

import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.util.ContainerUtils;
import io.arconia.testcontainers.redis.RedisContainer;

/**
 * A {@link RedisContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaRedisContainer extends RedisContainer {

    private static final String COMPATIBLE_IMAGE_NAME = "redis";

    private final RedisDevServicesProperties properties;

    static final int REDIS_PORT = 6379;

    public ArconiaRedisContainer(RedisDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        this.properties = properties;

        ContainerConfigurer.base(this, properties);
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isValidPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), REDIS_PORT);
        }
    }

}
