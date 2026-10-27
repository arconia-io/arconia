package io.arconia.testcontainers.phoenix;

import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * A {@link Container} for Phoenix.
 */
public class PhoenixContainer extends GenericContainer<PhoenixContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("arizephoenix/phoenix");

    public static final int GRPC_PORT = 4317;

    public static final int HTTP_PORT = 6006;

    public PhoenixContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        addExposedPorts(GRPC_PORT, HTTP_PORT);
        // Phoenix serves HTTP requests once its startup has completed, including the OTLP gRPC server,
        // and the readiness endpoint also checks the database.
        this.waitingFor(Wait.forHttp("/readyz").forPort(HTTP_PORT).forStatusCode(200));
    }

    @Override
    protected void configure() {
        super.configure();
        // Disables the telemetry pixels of the Phoenix UI. User-provided values take precedence.
        getEnvMap().putIfAbsent("PHOENIX_TELEMETRY_ENABLED", "false");
    }

    public String getPhoenixUrl() {
        return "http://" + getHost() + ":" + getHttpPort();
    }

    public Integer getGrpcPort() {
        return getMappedPort(GRPC_PORT);
    }

    public Integer getHttpPort() {
        return getMappedPort(HTTP_PORT);
    }

}
