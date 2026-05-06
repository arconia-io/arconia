package io.arconia.testcontainers.garage;

import java.net.URI;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

/**
 * A {@link Container} for Garage, an S3-compatible object store.
 *
 * <p>Garage requires post-startup initialization (cluster layout, key import, bucket creation)
 * before its S3 API is usable. The container performs that bootstrap automatically the first
 * time it starts and exposes deterministic credentials so that consumers can configure their
 * S3 clients statically.
 */
public class GarageContainer extends GenericContainer<GarageContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("dxflrs/garage");

    public static final int S3_API_PORT = 3900;
    public static final int RPC_PORT = 3901;
    public static final int S3_WEB_PORT = 3902;
    public static final int ADMIN_PORT = 3903;

    public static final String DEFAULT_IMAGE = "dxflrs/garage:v2.3.0";

    public static final String DEFAULT_REGION = "garage";
    @SuppressWarnings("java:S6419")
    public static final String DEFAULT_ACCESS_KEY = "GK00000000000000000000000a";
    @SuppressWarnings("java:S6419")
    public static final String DEFAULT_SECRET_KEY =
            "0000000000000000000000000000000000000000000000000000000000000000";
    public static final String DEFAULT_BUCKET = "arconia";

    private static final String GARAGE_CONFIG_PATH = "/etc/garage.toml";
    private static final String KEY_NAME = "arconia-key";

    // Intentional static fixtures for container config — not real secrets.
    // rpc_secret and admin_token must be valid hex strings of the right length.
    @SuppressWarnings("java:S6419")
    private static final String RPC_SECRET =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    @SuppressWarnings("java:S6419")
    private static final String ADMIN_TOKEN =
            "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";

    private String accessKey = DEFAULT_ACCESS_KEY;
    private String secretKey = DEFAULT_SECRET_KEY;
    private String defaultBucket = DEFAULT_BUCKET;

    public GarageContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        addExposedPorts(S3_API_PORT, S3_WEB_PORT, ADMIN_PORT);
        // Garage logs this line once the S3 API is listening; image has no shell so a
        // command-based wait strategy isn't viable.
        waitingFor(Wait.forLogMessage(".*S3 API server listening on .*\\n", 1));
    }

    public GarageContainer withCredentials(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        return self();
    }

    public GarageContainer withDefaultBucket(String bucket) {
        this.defaultBucket = bucket;
        return self();
    }

    @Override
    protected void configure() {
        super.configure();
        withCopyToContainer(Transferable.of(buildGarageConfig()), GARAGE_CONFIG_PATH);
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        super.containerIsStarted(containerInfo);
        bootstrap();
    }

    private void bootstrap() {
        try {
            String nodeId = exec("/garage", "node", "id", "-q").stdout().trim().split("@")[0];

            exec("/garage", "layout", "assign", "-z", "dc1", "-c", "1G", nodeId);
            exec("/garage", "layout", "apply", "--version", "1");

            exec("/garage", "key", "import", "--yes", "-n", KEY_NAME, accessKey, secretKey);
            exec("/garage", "bucket", "create", defaultBucket);
            exec("/garage", "bucket", "allow", "--read", "--write", "--owner",
                    "--key", KEY_NAME, defaultBucket);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to bootstrap Garage container", ex);
        }
    }

    private ExecResult exec(String... command) throws Exception {
        var result = execInContainer(command);
        if (result.getExitCode() != 0) {
            // Truncate to first 3 tokens to avoid leaking credential arguments (e.g. key import).
            String display = command.length > 3
                    ? command[0] + " " + command[1] + " " + command[2] + " ..."
                    : String.join(" ", command);
            throw new IllegalStateException(
                    "Garage command failed (exit %d): %s%nstdout: %s%nstderr: %s".formatted(
                            result.getExitCode(),
                            display,
                            result.getStdout(),
                            result.getStderr()));
        }
        return new ExecResult(result.getStdout(), result.getStderr());
    }

    private record ExecResult(String stdout, String stderr) {}

    private static String buildGarageConfig() {
        return """
                metadata_dir = "/var/lib/garage/meta"
                data_dir = "/var/lib/garage/data"

                replication_factor = 1

                rpc_bind_addr = "[::]:%d"
                rpc_public_addr = "127.0.0.1:%d"
                rpc_secret = "%s"

                [s3_api]
                s3_region = "%s"
                api_bind_addr = "[::]:%d"
                root_domain = ".s3.garage"

                [s3_web]
                bind_addr = "[::]:%d"
                root_domain = ".web.garage"
                index = "index.html"

                [admin]
                api_bind_addr = "[::]:%d"
                admin_token = "%s"
                """.formatted(
                        RPC_PORT, RPC_PORT, RPC_SECRET,
                        DEFAULT_REGION, S3_API_PORT,
                        S3_WEB_PORT,
                        ADMIN_PORT, ADMIN_TOKEN);
    }

    public String getS3Endpoint() {
        return "http://" + getHost() + ":" + getMappedPort(S3_API_PORT);
    }

    public URI getS3EndpointUri() {
        return URI.create(getS3Endpoint());
    }

    public Integer getS3Port() {
        return getMappedPort(S3_API_PORT);
    }

    public String getRegion() {
        return DEFAULT_REGION;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getDefaultBucket() {
        return defaultBucket;
    }

}
