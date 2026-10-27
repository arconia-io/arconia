package io.arconia.testcontainers.openlit;

import java.time.Duration;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.clickhouse.ClickHouseContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

/**
 * A {@link GenericContainer} for OpenLIT.
 * <p>
 * Manages an internal ClickHouse container, which OpenLIT uses as its backend database.
 * The ClickHouse container is started before OpenLIT and stopped together with it.
 * <p>
 * If a network is set on this container via {@link #withNetwork(Network)} before starting,
 * both OpenLIT and its ClickHouse instance will join that network. Otherwise, a private
 * network is created automatically, and it's removed when the container is stopped.
 */
public class OpenLitContainer extends GenericContainer<OpenLitContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("ghcr.io/openlit/openlit");

    private static final DockerImageName DEFAULT_CLICKHOUSE_IMAGE_NAME =
            DockerImageName.parse("clickhouse/clickhouse-server");

    private static final String CLICKHOUSE_NETWORK_ALIAS = "openlit-clickhouse";
    private static final String OTEL_COLLECTOR_CONFIG_PATH = "/etc/otel/otel-collector-config.yaml";

    private static final Duration DEFAULT_STARTUP_TIMEOUT = Duration.ofMinutes(2);

    // Named network used in reuse mode so the Docker network UUID is stable across JVM restarts,
    // keeping the container hash deterministic and allowing Testcontainers to find the existing container.
    private static final String REUSE_NETWORK_NAME = "arconia-openlit";

    // The OpenLIT image bundles the OpenTelemetry Collector but ships no configuration for it.
    // This matches the configuration mounted by the upstream docker-compose assets, with values
    // substituted at configure() time rather than relying on the Collector's env var expansion.
    private static final String OTEL_COLLECTOR_CONFIG_TEMPLATE = """
            receivers:
              otlp:
                protocols:
                  grpc:
                    endpoint: 0.0.0.0:4317
                  http:
                    endpoint: 0.0.0.0:4318

            processors:
              batch:
              memory_limiter:
                limit_mib: 1500
                spike_limit_mib: 512
                check_interval: 5s

            exporters:
              clickhouse:
                endpoint: tcp://%s:9000?dial_timeout=10s
                database: %s
                username: %s
                password: %s
                ttl: 730h
                logs_table_name: otel_logs
                traces_table_name: otel_traces
                timeout: 5s
                retry_on_failure:
                  enabled: true
                  initial_interval: 5s
                  max_interval: 30s
                  max_elapsed_time: 300s

            service:
              pipelines:
                logs:
                  receivers: [otlp]
                  processors: [batch]
                  exporters: [clickhouse]
                traces:
                  receivers: [otlp]
                  processors: [memory_limiter, batch]
                  exporters: [clickhouse]
                metrics:
                  receivers: [otlp]
                  processors: [memory_limiter, batch]
                  exporters: [clickhouse]
            """;

    public static final int UI_PORT = 3000;
    public static final int OTLP_GRPC_PORT = 4317;
    public static final int OTLP_HTTP_PORT = 4318;

    private DockerImageName clickHouseImageName = DEFAULT_CLICKHOUSE_IMAGE_NAME;

    private @Nullable ClickHouseContainer clickHouseContainer;

    private @Nullable Network privateNetwork;

    public OpenLitContainer(DockerImageName imageName) {
        super(imageName.asCompatibleSubstituteFor(DEFAULT_IMAGE_NAME));
        imageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        addExposedPorts(UI_PORT, OTLP_GRPC_PORT, OTLP_HTTP_PORT);
        // ClickHouse initialization and the Next.js startup make OpenLIT slow to come up.
        // The OTLP endpoint is checked explicitly because a custom wait strategy replaces the default
        // port check. The Collector answers GET with 405 on the OTLP paths and starts the gRPC listener
        // together with the HTTP one. An HTTP check is used instead of a port check because the latter
        // aborts on any transient Docker API error.
        this.waitingFor(new WaitAllStrategy()
                .withStrategy(Wait.forHttp("/").forPort(UI_PORT).forStatusCodeMatching(sc -> sc < 500))
                .withStrategy(Wait.forHttp("/v1/traces").forPort(OTLP_HTTP_PORT).forStatusCode(405))
                .withStartupTimeout(DEFAULT_STARTUP_TIMEOUT));
    }

    public OpenLitContainer withClickHouseImage(DockerImageName imageName) {
        this.clickHouseImageName = imageName.asCompatibleSubstituteFor(DEFAULT_CLICKHOUSE_IMAGE_NAME);
        return self();
    }

    public OpenLitContainer withClickHouseImage(String imageName) {
        return withClickHouseImage(DockerImageName.parse(imageName).asCompatibleSubstituteFor(DEFAULT_CLICKHOUSE_IMAGE_NAME));
    }

    @Override
    public void start() {
        if (clickHouseContainer == null) {
            // A network set before starting, such as the one shared by the Arconia Dev Services,
            // is used as it is and never closed here, since other containers may be attached to it.
            Network network = getNetwork();
            if (network == null) {
                if (isShouldBeReused()) {
                    network = resolveNamedNetwork();
                }
                else {
                    network = Network.newNetwork();
                    privateNetwork = network;
                }
                withNetwork(network);
            }

            clickHouseContainer = new ClickHouseContainer(
                    clickHouseImageName.asCompatibleSubstituteFor("clickhouse/clickhouse-server"))
                    .withNetwork(network)
                    .withNetworkAliases(CLICKHOUSE_NETWORK_ALIAS)
                    .withReuse(isShouldBeReused());
        }
        clickHouseContainer.start();
        super.start();
    }

    /**
     * Stops OpenLIT and its ClickHouse container, and removes the network created for them, if any.
     */
    @Override
    public void stop() {
        super.stop();
        if (clickHouseContainer != null) {
            clickHouseContainer.stop();
            clickHouseContainer = null;
        }
        if (privateNetwork != null) {
            privateNetwork.close();
            privateNetwork = null;
            setNetwork(null);
        }
    }

    @Override
    protected void configure() {
        super.configure();

        ClickHouseContainer clickHouseContainer = this.clickHouseContainer;
        if (clickHouseContainer == null) {
            throw new IllegalStateException("The ClickHouse container is created when the OpenLIT container starts");
        }

        String otelCollectorConfig = OTEL_COLLECTOR_CONFIG_TEMPLATE.formatted(
                CLICKHOUSE_NETWORK_ALIAS, clickHouseContainer.getDatabaseName(),
                clickHouseContainer.getUsername(), clickHouseContainer.getPassword());

        Map<String, String> env = getEnvMap();

        env.putIfAbsent("TELEMETRY_ENABLED", "false");

        // Sets the host address of the ClickHouse server for OpenLIT to connect.
        env.putIfAbsent("INIT_DB_HOST", CLICKHOUSE_NETWORK_ALIAS);
        // Sets the port on which ClickHouse listens.
        env.putIfAbsent("INIT_DB_PORT", "8123");
        // Sets the name of the database in Clickhouse to be used by OpenLIT.
        env.putIfAbsent("INIT_DB_DATABASE", clickHouseContainer.getDatabaseName());
        // Sets the username for authenticating with ClickHouse.
        env.putIfAbsent("INIT_DB_USERNAME", clickHouseContainer.getUsername());
        // Sets the password for authenticating with ClickHouse.
        env.putIfAbsent("INIT_DB_PASSWORD", clickHouseContainer.getPassword());

        // Sets the location where SQLITE data is stored.
        env.putIfAbsent("SQLITE_DATABASE_URL", "file:/app/client/data/data.db");
        this.withTmpFs(Map.of("/app/client/data", "rw"));

        // Demo account created by the OpenLIT seed, used for automatic sign-in.
        env.putIfAbsent("DEMO_ACCOUNT_EMAIL", "user@openlit.io");
        env.putIfAbsent("DEMO_ACCOUNT_PASSWORD", "openlituser");

        this.withCopyToContainer(Transferable.of(otelCollectorConfig), OTEL_COLLECTOR_CONFIG_PATH);
    }

    /**
     * Returns the logs of the internal ClickHouse container, useful when OpenLIT fails
     * because of its database.
     */
    public String getClickHouseLogs() {
        if (clickHouseContainer == null) {
            throw new IllegalStateException("The ClickHouse container is created when the OpenLIT container starts");
        }
        return clickHouseContainer.getLogs();
    }

    public String getOpenLitUrl() {
        return "http://" + getHost() + ":" + getUiPort();
    }

    public Integer getUiPort() {
        return getMappedPort(UI_PORT);
    }

    public Integer getOtlpGrpcPort() {
        return getMappedPort(OTLP_GRPC_PORT);
    }

    public Integer getOtlpHttpPort() {
        return getMappedPort(OTLP_HTTP_PORT);
    }

    public String getOtlpGrpcUrl() {
        return "http://" + getHost() + ":" + getOtlpGrpcPort();
    }

    public String getOtlpHttpUrl() {
        return "http://" + getHost() + ":" + getOtlpHttpPort();
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
