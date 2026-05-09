package io.arconia.testcontainers.openlit;

import java.util.Map;

import com.github.dockerjava.api.command.InspectContainerResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.clickhouse.ClickHouseContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

/**
 * A {@link GenericContainer} for OpenLit.
 * <p>
 * Manages an internal ClickHouse container, which OpenLit uses as its backend database.
 * The ClickHouse container is started automatically before OpenLit via Testcontainers'
 * dependency mechanism.
 * <p>
 * If a network is set on this container via {@link #withNetwork(Network)} before starting,
 * both OpenLit and its ClickHouse instance will join that network. Otherwise, a private
 * network is created automatically.
 */
public class OpenLitContainer extends GenericContainer<OpenLitContainer> {

    private static final Logger logger = LoggerFactory.getLogger(OpenLitContainer.class);

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("ghcr.io/openlit/openlit");

    private static final DockerImageName DEFAULT_CLICKHOUSE_IMAGE_NAME =
            DockerImageName.parse("clickhouse/clickhouse-server");

    static final String CLICKHOUSE_DATABASE = "openlit";
    static final String CLICKHOUSE_USERNAME = "default";
    static final String CLICKHOUSE_PASSWORD = "OPENLIT";

    private static final String CLICKHOUSE_NETWORK_ALIAS = "clickhouse";
    private static final String OTEL_COLLECTOR_CONFIG_PATH = "/etc/otel/otel-collector-config.yaml";

    // Named network used in reuse mode so the Docker network UUID is stable across JVM restarts,
    // keeping the container hash deterministic and allowing Testcontainers to find the existing container.
    private static final String REUSE_NETWORK_NAME = "arconia-openlit";

    // Matches the OTel Collector config from the upstream docker-compose assets, with values
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

    public OpenLitContainer(DockerImageName imageName) {
        super(imageName.asCompatibleSubstituteFor(DEFAULT_IMAGE_NAME));
        imageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        addExposedPorts(UI_PORT, OTLP_GRPC_PORT, OTLP_HTTP_PORT);
        waitingFor(Wait.forHttp("/").forPort(UI_PORT).forStatusCodeMatching(sc -> sc < 500));
    }

    public OpenLitContainer withClickHouseImage(DockerImageName imageName) {
        this.clickHouseImageName = imageName.asCompatibleSubstituteFor(DEFAULT_CLICKHOUSE_IMAGE_NAME);
        return self();
    }

    public OpenLitContainer withClickHouseImage(String imageName) {
        return withClickHouseImage(DockerImageName.parse(imageName).asCompatibleSubstituteFor(DEFAULT_CLICKHOUSE_IMAGE_NAME));
    }

    @Override
    protected void configure() {
        super.configure();

        Network network = getNetwork();
        if (network == null) {
            network = isShouldBeReused() ? resolveNamedNetwork() : Network.newNetwork();
            withNetwork(network);
        }

        ClickHouseContainer clickHouseContainer = new ClickHouseContainer(
                clickHouseImageName.asCompatibleSubstituteFor("clickhouse/clickhouse-server"))
                .withNetwork(network)
                .withNetworkAliases(CLICKHOUSE_NETWORK_ALIAS)
                .withUsername(CLICKHOUSE_USERNAME)
                .withPassword(CLICKHOUSE_PASSWORD)
                .withDatabaseName(CLICKHOUSE_DATABASE)
                .withReuse(isShouldBeReused());

        clickHouseContainer.start();
        dependsOn(clickHouseContainer);

        String otelCollectorConfig = OTEL_COLLECTOR_CONFIG_TEMPLATE.formatted(
                CLICKHOUSE_NETWORK_ALIAS, CLICKHOUSE_DATABASE, CLICKHOUSE_USERNAME, CLICKHOUSE_PASSWORD);

        withEnv("INIT_DB_HOST", CLICKHOUSE_NETWORK_ALIAS)
                .withEnv("INIT_DB_PORT", "8123")
                .withEnv("INIT_DB_DATABASE", CLICKHOUSE_DATABASE)
                .withEnv("INIT_DB_USERNAME", CLICKHOUSE_USERNAME)
                .withEnv("INIT_DB_PASSWORD", CLICKHOUSE_PASSWORD)
                .withEnv("SQLITE_DATABASE_URL", "file:/app/client/data/data.db")
                .withEnv("PORT", String.valueOf(UI_PORT))
                .withEnv("DOCKER_PORT", String.valueOf(UI_PORT))
                .withTmpFs(Map.of("/app/client/data", "rw"))
                .withCopyToContainer(Transferable.of(otelCollectorConfig), OTEL_COLLECTOR_CONFIG_PATH);
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        super.containerIsStarted(containerInfo);
        logger.info("OpenLit UI: {}", getOpenLitUrl());
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
