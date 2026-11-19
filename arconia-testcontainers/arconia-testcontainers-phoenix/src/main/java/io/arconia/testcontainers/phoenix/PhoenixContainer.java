package io.arconia.testcontainers.phoenix;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import io.arconia.core.annotations.Incubating;

/**
 * A {@link GenericContainer} for Arize Phoenix, an AI observability platform that accepts traces via OTLP.
 * <p>
 * Phoenix starts without authentication and with the telemetry of its UI turned off, unless the
 * {@code PHOENIX_ENABLE_AUTH} and {@code PHOENIX_TELEMETRY_ENABLED} environment variables are set.
 */
@Incubating
public class PhoenixContainer extends GenericContainer<PhoenixContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("arizephoenix/phoenix");

    /**
     * Port of the OTLP/gRPC endpoint.
     */
    public static final int OTLP_GRPC_PORT = 4317;

    /**
     * Port of the Phoenix HTTP server, which serves the UI, the APIs, and the OTLP/HTTP endpoint.
     */
    public static final int HTTP_PORT = 6006;

    /**
     * Creates a container for the given Phoenix image, compatible with {@code arizephoenix/phoenix}.
     */
    public PhoenixContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        addExposedPorts(OTLP_GRPC_PORT, HTTP_PORT);
        // Phoenix serves HTTP requests once its startup has completed, including the OTLP gRPC server,
        // and the readiness endpoint also checks the database.
        this.waitingFor(Wait.forHttp("/readyz").forPort(HTTP_PORT).forStatusCode(200));
    }

    /**
     * Creates a container for the given Phoenix image, compatible with {@code arizephoenix/phoenix}.
     */
    public PhoenixContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    @Override
    protected void configure() {
        super.configure();
        // Disables the telemetry pixels of the Phoenix UI. User-provided values take precedence.
        getEnvMap().putIfAbsent("PHOENIX_TELEMETRY_ENABLED", "false");
        // Phoenix enables authentication by default when listening on a non-loopback address, as in the image,
        // which would reject the telemetry data sent without an API key. User-provided values take precedence.
        getEnvMap().putIfAbsent("PHOENIX_ENABLE_AUTH", "false");
    }

    /**
     * Returns the URL of the Phoenix UI.
     */
    public String getPhoenixUrl() {
        return "http://" + getHost() + ":" + getHttpPort();
    }

    /**
     * Returns the host port mapped to {@link #OTLP_GRPC_PORT}.
     */
    public Integer getOtlpGrpcPort() {
        return getMappedPort(OTLP_GRPC_PORT);
    }

    /**
     * Returns the host port mapped to {@link #HTTP_PORT}.
     */
    public Integer getHttpPort() {
        return getMappedPort(HTTP_PORT);
    }

    /**
     * Returns the URL of the OTLP/gRPC endpoint.
     */
    public String getOtlpGrpcUrl() {
        return "http://" + getHost() + ":" + getOtlpGrpcPort();
    }

    /**
     * Returns the base URL of the OTLP/HTTP endpoint, to which the path {@code /v1/traces} is appended.
     */
    public String getOtlpHttpUrl() {
        return "http://" + getHost() + ":" + getHttpPort();
    }

}
