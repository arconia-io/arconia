package io.arconia.testcontainers.phoenix;

import com.github.dockerjava.api.command.InspectContainerResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * A {@link Container} for Phoenix.
 */
public class PhoenixContainer extends GenericContainer<PhoenixContainer> {

    private static final Logger logger = LoggerFactory.getLogger(PhoenixContainer.class);

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("arizephoenix/phoenix");

    private static final int DEFAULT_GRPC_PORT = 4317;

    private static final int DEFAULT_HTTP_PORT = 6006;

    public PhoenixContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        addExposedPorts(DEFAULT_GRPC_PORT, DEFAULT_HTTP_PORT);

        waitingFor(Wait.forLogMessage(".*Application startup complete.*", 1));
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        super.containerIsStarted(containerInfo);
        logger.info("Phoenix UI: {}", getPhoenixUrl());
    }

    public String getPhoenixUrl() {
        return "http://" + getHost() + ":" + getHttpPort();
    }

    public Integer getGrpcPort() {
        return getMappedPort(DEFAULT_GRPC_PORT);
    }

    public Integer getHttpPort() {
        return getMappedPort(DEFAULT_HTTP_PORT);
    }

}
