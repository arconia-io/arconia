package io.arconia.testcontainers.valkey;

import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * A {@link Container} for Valkey.
 */
public class ValkeyContainer extends GenericContainer<ValkeyContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("ghcr.io/valkey-io/valkey");

    public static final int VALKEY_PORT = 6379;

    public ValkeyContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        addExposedPorts(VALKEY_PORT);
        waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1));
    }

    public String getValkeyUrl() {
        return "redis://" + getHost() + ":" + getValkeyPort();
    }

    public Integer getValkeyPort() {
        return getMappedPort(VALKEY_PORT);
    }

}
