package io.arconia.testcontainers.langfuse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;

import io.floci.testcontainers.FlociContainer;

import org.jspecify.annotations.Nullable;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.clickhouse.ClickHouseContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.core.annotations.Incubating;
import io.arconia.testcontainers.redis.RedisContainer;

/**
 * A {@link GenericContainer} for Langfuse.
 * <p>
 * It runs the Langfuse web application and manages the containers it needs: the Langfuse
 * worker, PostgreSQL, ClickHouse, Redis, and Floci as S3-compatible blob storage. They are
 * started before Langfuse, except for the worker, which starts once Langfuse has run its database
 * migrations, and they're stopped together with it.
 * <p>
 * Langfuse is initialized with an organization, a project with API keys, and a user, so that
 * telemetry data can be sent and explored right away.
 * <p>
 * The Langfuse server closes plaintext HTTP connections that request an upgrade to HTTP/2 (h2c), as the
 * JDK HTTP client does by default, so the OTLP endpoint (see {@link #getOtlpTracesUrl()}) needs a client
 * using HTTP/1.1 (see <a href="https://github.com/open-telemetry/opentelemetry-java/issues/8982">opentelemetry-java#8982</a>).
 * <p>
 * If a network is set on this container via {@link #withNetwork(Network)} before starting,
 * Langfuse and the containers it needs join that network. Otherwise, a private network is
 * created automatically, and it's removed when the container is stopped.
 */
@Incubating
public class LangfuseContainer extends GenericContainer<LangfuseContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("langfuse/langfuse");

    private static final DockerImageName DEFAULT_WORKER_IMAGE_NAME = DockerImageName.parse("langfuse/langfuse-worker");

    private static final DockerImageName DEFAULT_POSTGRES_IMAGE_NAME = DockerImageName.parse("postgres");

    private static final DockerImageName DEFAULT_CLICKHOUSE_IMAGE_NAME = DockerImageName.parse("clickhouse/clickhouse-server");

    private static final DockerImageName DEFAULT_REDIS_IMAGE_NAME = DockerImageName.parse("redis");

    private static final DockerImageName DEFAULT_FLOCI_IMAGE_NAME = DockerImageName.parse("floci/floci");

    private static final String LANGFUSE_NETWORK_ALIAS = "langfuse";
    private static final String POSTGRES_NETWORK_ALIAS = "langfuse-postgres";
    private static final String CLICKHOUSE_NETWORK_ALIAS = "langfuse-clickhouse";
    private static final String REDIS_NETWORK_ALIAS = "langfuse-redis";
    private static final String FLOCI_NETWORK_ALIAS = "langfuse-floci";

    private static final String BUCKET_NAME = "langfuse";

    private static final int WORKER_HTTP_PORT = 3030;

    private static final Duration DEFAULT_STARTUP_TIMEOUT = Duration.ofMinutes(5);

    // Named network used in reuse mode so the Docker network UUID is stable across JVM restarts,
    // keeping the container hash deterministic and allowing Testcontainers to find the existing container.
    private static final String REUSE_NETWORK_NAME = "arconia-langfuse";

    /**
     * Port of the Langfuse web application, which serves the UI and the APIs, including OTLP.
     */
    public static final int HTTP_PORT = 3000;

    /**
     * Path of the OTLP/HTTP endpoint for traces.
     */
    public static final String OTLP_TRACES_PATH = "/api/public/otel/v1/traces";

    /**
     * Public API key of the Langfuse project, unless set via the {@code LANGFUSE_INIT_PROJECT_PUBLIC_KEY} environment variable.
     */
    public static final String DEFAULT_PUBLIC_KEY = "pk-lf-arconia";

    /**
     * Secret API key of the Langfuse project, unless set via the {@code LANGFUSE_INIT_PROJECT_SECRET_KEY} environment variable.
     */
    public static final String DEFAULT_SECRET_KEY = "sk-lf-arconia";

    /**
     * Email of the Langfuse user, unless set via the {@code LANGFUSE_INIT_USER_EMAIL} environment variable.
     */
    public static final String DEFAULT_USER_EMAIL = "arconia@example.com";

    /**
     * Password of the Langfuse user, unless set via the {@code LANGFUSE_INIT_USER_PASSWORD} environment variable.
     */
    public static final String DEFAULT_USER_PASSWORD = "password";

    private DockerImageName workerImageName = DEFAULT_WORKER_IMAGE_NAME;

    private DockerImageName postgresImageName = DEFAULT_POSTGRES_IMAGE_NAME;

    private DockerImageName clickHouseImageName = DEFAULT_CLICKHOUSE_IMAGE_NAME;

    private DockerImageName redisImageName = DEFAULT_REDIS_IMAGE_NAME;

    private DockerImageName flociImageName = DEFAULT_FLOCI_IMAGE_NAME;

    // The containers Langfuse needs, created when it starts and stopped together with it.
    private List<GenericContainer<?>> dependencies = List.of();

    // The environment variables set by default when Langfuse starts. Some depend on the containers it needs,
    // such as the mapped port of Floci, so they're removed when it stops and computed again on the next start.
    private final Set<String> defaultEnvNames = new HashSet<>();

    private @Nullable Network privateNetwork;

    /**
     * Creates a container for the given Langfuse image, compatible with {@code langfuse/langfuse}.
     */
    public LangfuseContainer(DockerImageName imageName) {
        super(imageName);
        imageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        addExposedPorts(HTTP_PORT);
        // Langfuse runs its database migrations at startup, which makes it slow to come up.
        this.waitingFor(Wait.forHttp("/api/public/health").forPort(HTTP_PORT).forStatusCode(200)
                .withStartupTimeout(DEFAULT_STARTUP_TIMEOUT));
    }

    /**
     * Creates a container for the given Langfuse image, compatible with {@code langfuse/langfuse}.
     */
    public LangfuseContainer(String imageName) {
        this(DockerImageName.parse(imageName));
    }

    /**
     * Sets the image of the Langfuse worker, compatible with {@code langfuse/langfuse-worker}.
     */
    public LangfuseContainer withWorkerImage(DockerImageName imageName) {
        this.workerImageName = imageName.asCompatibleSubstituteFor(DEFAULT_WORKER_IMAGE_NAME);
        return self();
    }

    /**
     * Sets the image of the Langfuse worker, compatible with {@code langfuse/langfuse-worker}.
     */
    public LangfuseContainer withWorkerImage(String imageName) {
        return withWorkerImage(DockerImageName.parse(imageName));
    }

    /**
     * Sets the image of PostgreSQL, compatible with {@code postgres}.
     */
    public LangfuseContainer withPostgresImage(DockerImageName imageName) {
        this.postgresImageName = imageName.asCompatibleSubstituteFor(DEFAULT_POSTGRES_IMAGE_NAME);
        return self();
    }

    /**
     * Sets the image of PostgreSQL, compatible with {@code postgres}.
     */
    public LangfuseContainer withPostgresImage(String imageName) {
        return withPostgresImage(DockerImageName.parse(imageName));
    }

    /**
     * Sets the image of ClickHouse, compatible with {@code clickhouse/clickhouse-server}.
     */
    public LangfuseContainer withClickHouseImage(DockerImageName imageName) {
        this.clickHouseImageName = imageName.asCompatibleSubstituteFor(DEFAULT_CLICKHOUSE_IMAGE_NAME);
        return self();
    }

    /**
     * Sets the image of ClickHouse, compatible with {@code clickhouse/clickhouse-server}.
     */
    public LangfuseContainer withClickHouseImage(String imageName) {
        return withClickHouseImage(DockerImageName.parse(imageName));
    }

    /**
     * Sets the image of Redis, compatible with {@code redis}.
     */
    public LangfuseContainer withRedisImage(DockerImageName imageName) {
        this.redisImageName = imageName.asCompatibleSubstituteFor(DEFAULT_REDIS_IMAGE_NAME);
        return self();
    }

    /**
     * Sets the image of Redis, compatible with {@code redis}.
     */
    public LangfuseContainer withRedisImage(String imageName) {
        return withRedisImage(DockerImageName.parse(imageName));
    }

    /**
     * Sets the image of Floci, compatible with {@code floci/floci}.
     */
    public LangfuseContainer withFlociImage(DockerImageName imageName) {
        this.flociImageName = imageName.asCompatibleSubstituteFor(DEFAULT_FLOCI_IMAGE_NAME);
        return self();
    }

    /**
     * Sets the image of Floci, compatible with {@code floci/floci}.
     */
    public LangfuseContainer withFlociImage(String imageName) {
        return withFlociImage(DockerImageName.parse(imageName));
    }

    /**
     * Starts the containers Langfuse needs, then Langfuse and its worker. If any of them fails to start,
     * the ones already started are stopped.
     */
    @Override
    public void start() {
        if (getContainerId() != null) {
            return;
        }

        // A network set before starting, such as the one shared by the Arconia Dev Services,
        // is used as it is and never closed here, since other containers may be attached to it.
        Network network = getNetwork();
        if (network == null) {
            if (isShouldBeReused()) {
                network = resolveNamedNetwork();
            } else {
                network = Network.newNetwork();
                privateNetwork = network;
            }
            withNetwork(network);
        }
        withNetworkAliases(LANGFUSE_NETWORK_ALIAS);

        boolean reuse = isShouldBeReused();
        PostgreSQLContainer postgres = new PostgreSQLContainer(postgresImageName)
                .withNetwork(network)
                .withNetworkAliases(POSTGRES_NETWORK_ALIAS)
                .withReuse(reuse);
        ClickHouseContainer clickHouse = new ClickHouseContainer(clickHouseImageName)
                .withNetwork(network)
                .withNetworkAliases(CLICKHOUSE_NETWORK_ALIAS)
                .withReuse(reuse);
        RedisContainer redis = new RedisContainer(redisImageName)
                .withNetwork(network)
                .withNetworkAliases(REDIS_NETWORK_ALIAS)
                .withReuse(reuse);
        // Only S3 is needed, as blob storage for the ingested events and media.
        FlociContainer floci = new FlociContainer(flociImageName)
                .disableAllServices()
                .withS3Config(s3 -> s3.enabled(true))
                .withNetwork(network)
                .withNetworkAliases(FLOCI_NETWORK_ALIAS)
                .withReuse(reuse);
        // The worker processes the ingested data, while the web application serves the UI and the APIs.
        // Its health check verifies that it reaches PostgreSQL and Redis, so that it doesn't fail unnoticed.
        GenericContainer<?> worker = new GenericContainer<>(workerImageName)
                .withNetwork(network)
                .withExposedPorts(WORKER_HTTP_PORT)
                .waitingFor(Wait.forHttp("/api/health").forPort(WORKER_HTTP_PORT).forStatusCode(200))
                .withReuse(reuse);
        dependencies = List.of(worker, postgres, clickHouse, redis, floci);

        try {
            Startables.deepStart(postgres, clickHouse, redis, floci).join();
            createBucket(floci);
            configureLangfuseEnvironment(postgres, clickHouse, floci);
            getEnvMap().forEach(worker::withEnv);

            super.start();
            // The worker initializes data in PostgreSQL, so it starts once Langfuse has run the database migrations.
            worker.start();
        } catch (RuntimeException ex) {
            // Nothing else stops the containers Langfuse needs when it fails to start.
            stop();
            // The containers started together report their failure wrapped in a CompletionException.
            throw (ex instanceof CompletionException && ex.getCause() instanceof RuntimeException cause) ? cause : ex;
        }
    }

    /**
     * Stops Langfuse and the containers it needs, and removes the network created for them, if any.
     */
    @Override
    public void stop() {
        super.stop();
        dependencies.forEach(GenericContainer::stop);
        dependencies = List.of();
        defaultEnvNames.forEach(getEnvMap()::remove);
        defaultEnvNames.clear();
        if (privateNetwork != null) {
            privateNetwork.close();
            privateNetwork = null;
            setNetwork(null);
        }
    }

    /**
     * Returns the URL of the Langfuse UI and APIs.
     */
    public String getLangfuseUrl() {
        return "http://" + getHost() + ":" + getHttpPort();
    }

    /**
     * Returns the host port mapped to {@link #HTTP_PORT}.
     */
    public Integer getHttpPort() {
        return getMappedPort(HTTP_PORT);
    }

    /**
     * Returns the URL of the OTLP/HTTP endpoint for traces, which is the only signal Langfuse accepts.
     * <p>
     * The Langfuse server closes plaintext HTTP connections that request an upgrade to HTTP/2 (h2c),
     * as the JDK HTTP client does by default, so the requests must use HTTP/1.1.
     */
    public String getOtlpTracesUrl() {
        return getLangfuseUrl() + OTLP_TRACES_PATH;
    }

    /**
     * Returns the public API key of the Langfuse project.
     */
    public String getPublicKey() {
        return getEnvMap().getOrDefault("LANGFUSE_INIT_PROJECT_PUBLIC_KEY", DEFAULT_PUBLIC_KEY);
    }

    /**
     * Returns the secret API key of the Langfuse project.
     */
    public String getSecretKey() {
        return getEnvMap().getOrDefault("LANGFUSE_INIT_PROJECT_SECRET_KEY", DEFAULT_SECRET_KEY);
    }

    /**
     * Returns the value of the {@code Authorization} header for the Langfuse public API, including OTLP.
     */
    public String getBasicAuthHeader() {
        String credentials = getPublicKey() + ":" + getSecretKey();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Configures the environment shared by the Langfuse web application and worker.
     * User-provided values take precedence.
     */
    private void configureLangfuseEnvironment(PostgreSQLContainer postgres, ClickHouseContainer clickHouse,
            FlociContainer floci) {
        putDefaultEnv("TELEMETRY_ENABLED", "false");
        // Langfuse waits 15 seconds by default before processing the ingested events.
        // A shorter delay makes the telemetry data visible right away during development and testing.
        putDefaultEnv("LANGFUSE_INGESTION_QUEUE_DELAY_MS", "1000");
        putDefaultEnv("NEXTAUTH_URL", "http://localhost:" + HTTP_PORT);
        putDefaultEnv("NEXTAUTH_SECRET", "arconia");
        putDefaultEnv("SALT", "arconia");
        putDefaultEnv("ENCRYPTION_KEY", "0".repeat(64));

        putDefaultEnv("DATABASE_URL", "postgresql://%s:%s@%s:5432/%s".formatted(postgres.getUsername(),
                postgres.getPassword(), POSTGRES_NETWORK_ALIAS, postgres.getDatabaseName()));

        putDefaultEnv("CLICKHOUSE_MIGRATION_URL", "clickhouse://%s:9000".formatted(CLICKHOUSE_NETWORK_ALIAS));
        putDefaultEnv("CLICKHOUSE_URL", "http://%s:8123".formatted(CLICKHOUSE_NETWORK_ALIAS));
        putDefaultEnv("CLICKHOUSE_USER", clickHouse.getUsername());
        putDefaultEnv("CLICKHOUSE_PASSWORD", clickHouse.getPassword());
        putDefaultEnv("CLICKHOUSE_CLUSTER_ENABLED", "false");

        putDefaultEnv("REDIS_HOST", REDIS_NETWORK_ALIAS);
        putDefaultEnv("REDIS_PORT", String.valueOf(RedisContainer.REDIS_PORT));

        String s3Endpoint = "http://%s:%d".formatted(FLOCI_NETWORK_ALIAS, FlociContainer.PORT);
        for (String prefix : List.of("LANGFUSE_S3_EVENT_UPLOAD", "LANGFUSE_S3_MEDIA_UPLOAD")) {
            putDefaultEnv(prefix + "_BUCKET", BUCKET_NAME);
            putDefaultEnv(prefix + "_REGION", floci.getRegion());
            putDefaultEnv(prefix + "_ACCESS_KEY_ID", floci.getAccessKey());
            putDefaultEnv(prefix + "_SECRET_ACCESS_KEY", floci.getSecretKey());
            putDefaultEnv(prefix + "_FORCE_PATH_STYLE", "true");
        }
        putDefaultEnv("LANGFUSE_S3_EVENT_UPLOAD_ENDPOINT", s3Endpoint);
        // Media is uploaded and downloaded from the host via presigned URLs, built with the external endpoint,
        // while Langfuse itself reaches the storage via the internal endpoint.
        putDefaultEnv("LANGFUSE_S3_MEDIA_UPLOAD_ENDPOINT",
                "http://%s:%d".formatted(floci.getHost(), floci.getMappedPort(FlociContainer.PORT)));
        putDefaultEnv("LANGFUSE_S3_MEDIA_UPLOAD_INTERNAL_ENDPOINT", s3Endpoint);
        putDefaultEnv("LANGFUSE_S3_EVENT_UPLOAD_PREFIX", "events/");
        putDefaultEnv("LANGFUSE_S3_MEDIA_UPLOAD_PREFIX", "media/");

        putDefaultEnv("LANGFUSE_INIT_ORG_ID", "arconia");
        putDefaultEnv("LANGFUSE_INIT_ORG_NAME", "Arconia");
        putDefaultEnv("LANGFUSE_INIT_PROJECT_ID", "arconia");
        putDefaultEnv("LANGFUSE_INIT_PROJECT_NAME", "Arconia");
        putDefaultEnv("LANGFUSE_INIT_PROJECT_PUBLIC_KEY", DEFAULT_PUBLIC_KEY);
        putDefaultEnv("LANGFUSE_INIT_PROJECT_SECRET_KEY", DEFAULT_SECRET_KEY);
        putDefaultEnv("LANGFUSE_INIT_USER_EMAIL", DEFAULT_USER_EMAIL);
        putDefaultEnv("LANGFUSE_INIT_USER_NAME", "Arconia");
        putDefaultEnv("LANGFUSE_INIT_USER_PASSWORD", DEFAULT_USER_PASSWORD);
    }

    /**
     * Sets the environment variable unless the user has set it, and remembers it as a default,
     * so that it's removed when the container stops and computed again when it starts.
     */
    private void putDefaultEnv(String name, String value) {
        if (getEnvMap().putIfAbsent(name, value) == null) {
            defaultEnvNames.add(name);
        }
    }

    /**
     * Creates the bucket Langfuse stores events and media in, since Langfuse doesn't create it.
     * Floci accepts unsigned requests by default.
     */
    private static void createBucket(FlociContainer floci) {
        URI bucketUri = URI.create("http://%s:%d/%s".formatted(floci.getHost(), floci.getMappedPort(FlociContainer.PORT), BUCKET_NAME));
        HttpResponse<String> response = send(HttpRequest.newBuilder(bucketUri).PUT(HttpRequest.BodyPublishers.noBody()).build(),
                "create the Langfuse bucket in Floci");
        if (response.statusCode() >= 300 && response.statusCode() != 409) {
            throw new IllegalStateException("Failed to create the Langfuse bucket in Floci (HTTP %d): %s"
                    .formatted(response.statusCode(), response.body()));
        }
    }

    private static HttpResponse<String> send(HttpRequest request, String action) {
        try (HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()) {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while trying to " + action, ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to " + action, ex);
        }
    }

    // Finds the named Docker network if it already exists, otherwise creates it.
    // Using a named network in reuse mode gives a stable Docker UUID so the container hash
    // stays the same across JVM restarts and Testcontainers can recognise the existing container.
    private Network resolveNamedNetwork() {
        var client = DockerClientFactory.lazyClient();
        String id = client.listNetworksCmd()
                .withNameFilter(REUSE_NETWORK_NAME)
                .exec()
                .stream()
                .filter(n -> REUSE_NETWORK_NAME.equals(n.getName()))
                .findFirst()
                .map(n -> n.getId())
                .orElseGet(() -> client.createNetworkCmd()
                        .withName(REUSE_NETWORK_NAME)
                        .withDriver("bridge")
                        .exec()
                        .getId());
        return new ExistingDockerNetwork(id);
    }

    private record ExistingDockerNetwork(String id) implements Network {
        @Override
        public String getId() {
            return id;
        }

        @Override
        public void close() {}
    }

}
