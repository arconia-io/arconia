package io.arconia.testcontainers.redis;

import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * A {@link Container} for Redis.
 */
public class RedisContainer extends GenericContainer<RedisContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("redis");

    private static final int DEFAULT_REDIS_PORT = 5672;

    public RedisContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        addExposedPorts(DEFAULT_REDIS_PORT);

        waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1));
    }

    public String getRedisUrl() {
        return "redis://" + getHost() + ":" + getRedisPort();
    }

    public Integer getRedisPort() {
        return getMappedPort(DEFAULT_REDIS_PORT);
    }

}
