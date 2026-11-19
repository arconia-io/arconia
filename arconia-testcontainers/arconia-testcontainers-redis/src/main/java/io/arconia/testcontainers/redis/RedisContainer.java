package io.arconia.testcontainers.redis;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;

import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.utility.DockerImageName;

import io.arconia.core.annotations.Incubating;

/**
 * A {@link GenericContainer} for Redis.
 * <p>
 * The container is ready once Redis accepts connections on the port mapped to the host, which can lag behind
 * the readiness of Redis itself with Docker Desktop and some Docker-in-Docker setups.
 */
@Incubating
public class RedisContainer extends GenericContainer<RedisContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("redis");

    private static final Duration DEFAULT_STARTUP_TIMEOUT = Duration.ofSeconds(60);

    /**
     * Port on which Redis accepts connections.
     */
    public static final int REDIS_PORT = 6379;

    /**
     * Creates a container for the given Redis image, compatible with {@code redis}.
     */
    public RedisContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        addExposedPorts(REDIS_PORT);
        // The log message signals that Redis is ready inside the container, and the mapped port check
        // that clients can reach it from the host.
        this.waitingFor(new WaitAllStrategy()
                .withStrategy(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1))
                .withStrategy(new MappedPortWaitStrategy())
                .withStartupTimeout(DEFAULT_STARTUP_TIMEOUT));
    }

    /**
     * Creates a container for the given Redis image, compatible with {@code redis}.
     */
    public RedisContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    /**
     * Returns the URL for connecting to Redis, such as {@code redis://localhost:32768}.
     */
    public String getRedisUrl() {
        return "redis://" + getHost() + ":" + getRedisPort();
    }

    /**
     * Returns the host port mapped to {@link #REDIS_PORT}.
     */
    public Integer getRedisPort() {
        return getMappedPort(REDIS_PORT);
    }

    /**
     * Waits until the mapped port of Redis accepts connections from the host. With Docker Desktop and
     * some Docker-in-Docker setups, the port is published through a proxy that can lag behind the container,
     * so clients connecting right after Redis is ready would be refused. Unlike the port check of
     * Testcontainers, it keeps trying until the startup timeout, also on transient errors of the
     * container runtime.
     */
    private static final class MappedPortWaitStrategy extends AbstractWaitStrategy {

        private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(1);

        private static final Duration RETRY_INTERVAL = Duration.ofMillis(100);

        @Override
        protected void waitUntilReady() {
            String host = waitStrategyTarget.getHost();
            int port = waitStrategyTarget.getMappedPort(REDIS_PORT);
            Instant deadline = Instant.now().plus(startupTimeout);
            while (true) {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(host, port), (int) CONNECT_TIMEOUT.toMillis());
                    return;
                } catch (IOException ex) {
                    if (Instant.now().isAfter(deadline)) {
                        throw new ContainerLaunchException(
                                "Timed out waiting for Redis to accept connections on %s:%d".formatted(host, port), ex);
                    }
                }
                try {
                    Thread.sleep(RETRY_INTERVAL);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new ContainerLaunchException("Interrupted while waiting for Redis to accept connections", ex);
                }
            }
        }

    }

}
