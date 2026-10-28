package io.arconia.testcontainers.langfuse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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

import io.arconia.testcontainers.redis.RedisContainer;

/**
 * A {@link GenericContainer} for Langfuse.
 * <p>
 * It runs the Langfuse web application and manages the containers it needs: the Langfuse
 * worker, PostgreSQL, ClickHouse, Redis, and Floci as S3-compatible blob storage. They are
 * started before Langfuse and stopped together with it.
 * <p>
 * Langfuse is initialized with an organization, a project with API keys, and a user, so that
 * telemetry data can be sent and explored right away.
 * <p>
 * The OTLP endpoint is exposed through a proxy in Floci (see {@link #getOtlpTracesUrl()}), because
 * the Langfuse server closes plaintext HTTP connections that request an upgrade to HTTP/2 (h2c), as the
 * JDK HTTP client does by default, including the one used by the OpenTelemetry Java exporters.
 * <p>
 * If a network is set on this container via {@link #withNetwork(Network)} before starting,
 * Langfuse and the containers it needs join that network. Otherwise, a private network is
 * created automatically, and it's removed when the container is stopped.
 */
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

    private static final Duration DEFAULT_STARTUP_TIMEOUT = Duration.ofMinutes(5);

    // Named network used in reuse mode so the Docker network UUID is stable across JVM restarts,
    // keeping the container hash deterministic and allowing Testcontainers to find the existing container.
    private static final String REUSE_NETWORK_NAME = "arconia-langfuse";

    public static final int HTTP_PORT = 3000;

    private static final String OTLP_TRACES_PATH = "/api/public/otel/v1/traces";

    private static final String OTLP_PROXY_NAME = "langfuse-otlp";

    private static final Pattern API_ID_PATTERN = Pattern.compile("\"apiId\"\\s*:\\s*\"([^\"]+)\"");

    private static final Pattern OTLP_PROXY_PATTERN = Pattern.compile(
            "\\{[^{}]*\"apiId\"\\s*:\\s*\"([^\"]+)\"[^{}]*\"name\"\\s*:\\s*\"" + OTLP_PROXY_NAME + "\"[^{}]*}");

    /**
     * Label with the host port of the OTLP proxy for traces, so that other applications can discover it.
     */
    public static final String OTLP_TRACES_PORT_LABEL = "io.arconia.langfuse.otlp-traces-port";

    /**
     * Label with the path of the OTLP proxy for traces, so that other applications can discover it.
     */
    public static final String OTLP_TRACES_PATH_LABEL = "io.arconia.langfuse.otlp-traces-path";

    public static final String DEFAULT_PUBLIC_KEY = "pk-lf-arconia";

    public static final String DEFAULT_SECRET_KEY = "sk-lf-arconia";

    public static final String DEFAULT_USER_EMAIL = "arconia@example.com";

    public static final String DEFAULT_USER_PASSWORD = "password";

    private DockerImageName workerImageName = DEFAULT_WORKER_IMAGE_NAME;

    private DockerImageName postgresImageName = DEFAULT_POSTGRES_IMAGE_NAME;

    private DockerImageName clickHouseImageName = DEFAULT_CLICKHOUSE_IMAGE_NAME;

    private DockerImageName redisImageName = DEFAULT_REDIS_IMAGE_NAME;

    private DockerImageName flociImageName = DEFAULT_FLOCI_IMAGE_NAME;

    private @Nullable GenericContainer<?> workerContainer;

    private @Nullable PostgreSQLContainer postgresContainer;

    private @Nullable ClickHouseContainer clickHouseContainer;

    private @Nullable RedisContainer redisContainer;

    private @Nullable FlociContainer flociContainer;

    private @Nullable Network privateNetwork;


    public LangfuseContainer(DockerImageName imageName) {
        super(imageName.asCompatibleSubstituteFor(DEFAULT_IMAGE_NAME));
        imageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        addExposedPorts(HTTP_PORT);
        // Langfuse runs its database migrations at startup, which makes it slow to come up.
        waitingFor(Wait.forHttp("/api/public/health").forPort(HTTP_PORT).forStatusCode(200)
                .withStartupTimeout(DEFAULT_STARTUP_TIMEOUT));
    }

    public LangfuseContainer withWorkerImage(DockerImageName imageName) {
        this.workerImageName = imageName.asCompatibleSubstituteFor(DEFAULT_WORKER_IMAGE_NAME);
        return self();
    }

    public LangfuseContainer withWorkerImage(String imageName) {
        return withWorkerImage(DockerImageName.parse(imageName));
    }

    public LangfuseContainer withPostgresImage(DockerImageName imageName) {
        this.postgresImageName = imageName.asCompatibleSubstituteFor(DEFAULT_POSTGRES_IMAGE_NAME);
        return self();
    }

    public LangfuseContainer withPostgresImage(String imageName) {
        return withPostgresImage(DockerImageName.parse(imageName));
    }

    public LangfuseContainer withClickHouseImage(DockerImageName imageName) {
        this.clickHouseImageName = imageName.asCompatibleSubstituteFor(DEFAULT_CLICKHOUSE_IMAGE_NAME);
        return self();
    }

    public LangfuseContainer withClickHouseImage(String imageName) {
        return withClickHouseImage(DockerImageName.parse(imageName));
    }

    public LangfuseContainer withRedisImage(DockerImageName imageName) {
        this.redisImageName = imageName.asCompatibleSubstituteFor(DEFAULT_REDIS_IMAGE_NAME);
        return self();
    }

    public LangfuseContainer withRedisImage(String imageName) {
        return withRedisImage(DockerImageName.parse(imageName));
    }

    public LangfuseContainer withFlociImage(DockerImageName imageName) {
        this.flociImageName = imageName.asCompatibleSubstituteFor(DEFAULT_FLOCI_IMAGE_NAME);
        return self();
    }

    public LangfuseContainer withFlociImage(String imageName) {
        return withFlociImage(DockerImageName.parse(imageName));
    }

    @Override
    public void start() {
        if (postgresContainer == null) {
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
            createContainers(network);
        }

        PostgreSQLContainer postgres = requireContainer(postgresContainer);
        ClickHouseContainer clickHouse = requireContainer(clickHouseContainer);
        FlociContainer floci = requireContainer(flociContainer);

        Startables.deepStart(postgres, clickHouse, requireContainer(redisContainer), floci).join();
        createBucket(floci);
        configureLangfuseEnvironment(postgres, clickHouse, floci);
        // The OTLP proxy is created before Langfuse starts, since its location is recorded in the labels.
        withLabel(OTLP_TRACES_PORT_LABEL, String.valueOf(floci.getMappedPort(FlociContainer.PORT)));
        withLabel(OTLP_TRACES_PATH_LABEL, "/_aws/execute-api/%s/v1/traces".formatted(findOrCreateOtlpProxy(floci)));

        // The worker processes the ingested data, while the web application serves the UI and the APIs.
        GenericContainer<?> worker = requireContainer(workerContainer);
        getEnvMap().forEach(worker::withEnv);
        worker.start();
        super.start();
    }

    /**
     * Stops Langfuse and the containers it needs, and removes the network created for them, if any.
     */
    @Override
    public void stop() {
        super.stop();
        for (GenericContainer<?> container : dependencies()) {
            container.stop();
        }
        workerContainer = null;
        postgresContainer = null;
        clickHouseContainer = null;
        redisContainer = null;
        flociContainer = null;
        if (privateNetwork != null) {
            privateNetwork.close();
            privateNetwork = null;
            setNetwork(null);
        }
    }

    public String getLangfuseUrl() {
        return "http://" + getHost() + ":" + getHttpPort();
    }

    public Integer getHttpPort() {
        return getMappedPort(HTTP_PORT);
    }

    /**
     * Returns the URL of the OTLP/HTTP endpoint for traces, which is the only signal Langfuse accepts.
     * <p>
     * It's a proxy in Floci to the Langfuse endpoint, since the Langfuse server closes plaintext HTTP
     * connections that request an upgrade to HTTP/2 (h2c), as the JDK HTTP client does by default.
     */
    public String getOtlpTracesUrl() {
        return "http://%s:%s%s".formatted(getHost(), requireContainer(getLabels().get(OTLP_TRACES_PORT_LABEL)),
                requireContainer(getLabels().get(OTLP_TRACES_PATH_LABEL)));
    }

    public String getPublicKey() {
        return getEnvMap().getOrDefault("LANGFUSE_INIT_PROJECT_PUBLIC_KEY", DEFAULT_PUBLIC_KEY);
    }

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

    private void createContainers(Network network) {
        boolean reuse = isShouldBeReused();
        postgresContainer = new PostgreSQLContainer(postgresImageName.asCompatibleSubstituteFor("postgres"))
                .withNetwork(network)
                .withNetworkAliases(POSTGRES_NETWORK_ALIAS)
                .withReuse(reuse);
        clickHouseContainer = new ClickHouseContainer(clickHouseImageName.asCompatibleSubstituteFor("clickhouse/clickhouse-server"))
                .withNetwork(network)
                .withNetworkAliases(CLICKHOUSE_NETWORK_ALIAS)
                .withReuse(reuse);
        redisContainer = new RedisContainer(redisImageName.asCompatibleSubstituteFor("redis"))
                .withNetwork(network)
                .withNetworkAliases(REDIS_NETWORK_ALIAS)
                .withReuse(reuse);
        // Only S3 is needed, as blob storage for the ingested events and media,
        // and API Gateway, as a proxy for the OTLP endpoint.
        flociContainer = new FlociContainer(flociImageName.asCompatibleSubstituteFor("floci/floci"))
                .disableAllServices()
                .withS3Config(s3 -> s3.enabled(true))
                .withApiGatewayConfig(apiGateway -> apiGateway.enabled(true))
                .withApiGatewayV2Config(apiGateway -> apiGateway.enabled(true))
                .withNetwork(network)
                .withNetworkAliases(FLOCI_NETWORK_ALIAS)
                .withReuse(reuse);
        workerContainer = new GenericContainer<>(workerImageName.asCompatibleSubstituteFor("langfuse/langfuse-worker"))
                .withNetwork(network)
                .withReuse(reuse);
    }

    /**
     * Configures the environment shared by the Langfuse web application and worker.
     * User-provided values take precedence.
     */
    private void configureLangfuseEnvironment(PostgreSQLContainer postgres, ClickHouseContainer clickHouse,
            FlociContainer floci) {
        Map<String, String> env = getEnvMap();

        env.putIfAbsent("TELEMETRY_ENABLED", "false");
        // Langfuse waits 15 seconds by default before processing the ingested events.
        // A shorter delay makes the telemetry data visible right away during development and testing.
        env.putIfAbsent("LANGFUSE_INGESTION_QUEUE_DELAY_MS", "1000");
        env.putIfAbsent("NEXTAUTH_URL", "http://localhost:" + HTTP_PORT);
        env.putIfAbsent("NEXTAUTH_SECRET", "arconia");
        env.putIfAbsent("SALT", "arconia");
        env.putIfAbsent("ENCRYPTION_KEY", "0".repeat(64));

        env.putIfAbsent("DATABASE_URL", "postgresql://%s:%s@%s:5432/%s".formatted(postgres.getUsername(),
                postgres.getPassword(), POSTGRES_NETWORK_ALIAS, postgres.getDatabaseName()));

        env.putIfAbsent("CLICKHOUSE_MIGRATION_URL", "clickhouse://%s:9000".formatted(CLICKHOUSE_NETWORK_ALIAS));
        env.putIfAbsent("CLICKHOUSE_URL", "http://%s:8123".formatted(CLICKHOUSE_NETWORK_ALIAS));
        env.putIfAbsent("CLICKHOUSE_USER", clickHouse.getUsername());
        env.putIfAbsent("CLICKHOUSE_PASSWORD", clickHouse.getPassword());
        env.putIfAbsent("CLICKHOUSE_CLUSTER_ENABLED", "false");

        env.putIfAbsent("REDIS_HOST", REDIS_NETWORK_ALIAS);
        env.putIfAbsent("REDIS_PORT", String.valueOf(RedisContainer.REDIS_PORT));

        String s3Endpoint = "http://%s:%d".formatted(FLOCI_NETWORK_ALIAS, FlociContainer.PORT);
        for (String prefix : List.of("LANGFUSE_S3_EVENT_UPLOAD", "LANGFUSE_S3_MEDIA_UPLOAD")) {
            env.putIfAbsent(prefix + "_BUCKET", BUCKET_NAME);
            env.putIfAbsent(prefix + "_REGION", floci.getRegion());
            env.putIfAbsent(prefix + "_ACCESS_KEY_ID", floci.getAccessKey());
            env.putIfAbsent(prefix + "_SECRET_ACCESS_KEY", floci.getSecretKey());
            env.putIfAbsent(prefix + "_ENDPOINT", s3Endpoint);
            env.putIfAbsent(prefix + "_FORCE_PATH_STYLE", "true");
        }
        env.putIfAbsent("LANGFUSE_S3_EVENT_UPLOAD_PREFIX", "events/");
        env.putIfAbsent("LANGFUSE_S3_MEDIA_UPLOAD_PREFIX", "media/");

        env.putIfAbsent("LANGFUSE_INIT_ORG_ID", "arconia");
        env.putIfAbsent("LANGFUSE_INIT_ORG_NAME", "Arconia");
        env.putIfAbsent("LANGFUSE_INIT_PROJECT_ID", "arconia");
        env.putIfAbsent("LANGFUSE_INIT_PROJECT_NAME", "Arconia");
        env.putIfAbsent("LANGFUSE_INIT_PROJECT_PUBLIC_KEY", DEFAULT_PUBLIC_KEY);
        env.putIfAbsent("LANGFUSE_INIT_PROJECT_SECRET_KEY", DEFAULT_SECRET_KEY);
        env.putIfAbsent("LANGFUSE_INIT_USER_EMAIL", DEFAULT_USER_EMAIL);
        env.putIfAbsent("LANGFUSE_INIT_USER_NAME", "Arconia");
        env.putIfAbsent("LANGFUSE_INIT_USER_PASSWORD", DEFAULT_USER_PASSWORD);
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

    /**
     * Returns the ID of the HTTP API in Floci that proxies requests to the OTLP endpoint for traces of
     * Langfuse, creating it if needed. Reusing the existing one keeps the labels, and therefore the
     * container, the same across restarts when reusing containers. Floci forwards requests over HTTP/1.1
     * without the request path, hence the full endpoint URL as target.
     */
    private static String findOrCreateOtlpProxy(FlociContainer floci) {
        URI apisUri = URI.create("http://%s:%d/v2/apis".formatted(floci.getHost(), floci.getMappedPort(FlociContainer.PORT)));
        HttpResponse<String> existing = send(HttpRequest.newBuilder(apisUri).GET().build(), "list the APIs in Floci");
        Matcher existingMatcher = OTLP_PROXY_PATTERN.matcher(existing.body());
        if (existing.statusCode() < 300 && existingMatcher.find()) {
            return existingMatcher.group(1);
        }

        String api = """
                {"name":"%s","protocolType":"HTTP","target":"http://%s:%d%s"}"""
                .formatted(OTLP_PROXY_NAME, LANGFUSE_NETWORK_ALIAS, HTTP_PORT, OTLP_TRACES_PATH);
        HttpResponse<String> response = send(HttpRequest.newBuilder(apisUri)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(api))
                .build(), "create the OTLP proxy in Floci");
        Matcher matcher = API_ID_PATTERN.matcher(response.body());
        if (response.statusCode() >= 300 || !matcher.find()) {
            throw new IllegalStateException("Failed to create the OTLP proxy in Floci (HTTP %d): %s"
                    .formatted(response.statusCode(), response.body()));
        }
        return matcher.group(1);
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

    private List<GenericContainer<?>> dependencies() {
        return Stream.of(workerContainer, postgresContainer, clickHouseContainer, redisContainer, flociContainer)
                .<GenericContainer<?>>map(container -> container)
                .filter(Objects::nonNull)
                .toList();
    }

    private static <T> T requireContainer(@Nullable T container) {
        if (container == null) {
            throw new IllegalStateException("The containers needed by Langfuse are created when the Langfuse container starts");
        }
        return container;
    }

    // Finds the named Docker network if it already exists, otherwise creates it.
    // Using a named network in reuse mode gives a stable Docker UUID so the container hash
    // stays the same across JVM restarts and Testcontainers can recognise the existing container.
    private Network resolveNamedNetwork() {
        var client = DockerClientFactory.lazyClient();
        var existing = client.listNetworksCmd()
                .withNameFilter(REUSE_NETWORK_NAME)
                .exec()
                .stream()
                .filter(n -> REUSE_NETWORK_NAME.equals(n.getName()))
                .toList();
        String id;
        if (!existing.isEmpty()) {
            id = existing.getFirst().getId();
        } else {
            id = client.createNetworkCmd()
                    .withName(REUSE_NETWORK_NAME)
                    .withDriver("bridge")
                    .exec()
                    .getId();
        }
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
