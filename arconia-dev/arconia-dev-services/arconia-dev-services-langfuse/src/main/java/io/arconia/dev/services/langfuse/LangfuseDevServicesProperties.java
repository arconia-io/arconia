package io.arconia.dev.services.langfuse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.arconia.dev.services.api.config.DevServicesProperties;
import io.arconia.dev.services.api.config.ResourceMapping;
import io.arconia.dev.services.api.config.ReuseStrategy;
import io.arconia.dev.services.api.config.VolumeMapping;

/**
 * Properties for the Langfuse Dev Services.
 */
@ConfigurationProperties(prefix = LangfuseDevServicesProperties.CONFIG_PREFIX)
public class LangfuseDevServicesProperties implements DevServicesProperties {

    public static final String CONFIG_PREFIX = "arconia.dev.services.langfuse";

    /**
     * Whether the dev service is enabled.
     */
    private boolean enabled = true;

    /**
     * Full name of the container image used in the dev service.
     */
    private String imageName = "langfuse/langfuse:4.71";

    /**
     * Full name of the container image used for the Langfuse worker.
     */
    private String workerImageName = "langfuse/langfuse-worker:4.71";

    /**
     * Full name of the container image used for the internal PostgreSQL instance.
     */
    private String postgresImageName = "postgres:18.6-alpine";

    /**
     * Full name of the container image used for the internal ClickHouse instance.
     */
    private String clickhouseImageName = "clickhouse/clickhouse-server:26.10-distroless";

    /**
     * Full name of the container image used for the internal Redis instance.
     */
    private String redisImageName = "redis:8.10-alpine";

    /**
     * Full name of the container image used for the internal Floci instance,
     * providing the S3-compatible storage and the proxy for the OTLP endpoint.
     */
    private String flociImageName = "floci/floci:2.3.0";

    /**
     * Public key of the API keys of the Langfuse project, used to authenticate the telemetry data sent to Langfuse.
     */
    private String publicKey = "pk-lf-arconia";

    /**
     * Secret key of the API keys of the Langfuse project, used to authenticate the telemetry data sent to Langfuse.
     */
    private String secretKey = "sk-lf-arconia";

    /**
     * Environment variables to set in the service.
     */
    private Map<String, String> environment = new HashMap<>();

    /**
     * Additional hostnames the dev service container is reachable by on the shared network,
     * next to the name of the dev service. Only applicable when the shared network is enabled.
     */
    private List<String> networkAliases = new ArrayList<>();

    /**
     * Fixed port for exposing the Langfuse UI and API to the host.
     * When it's 0 (default), a random available port is assigned dynamically.
     */
    private int port = 0;

    /**
     * Resources from the classpath or host filesystem to copy into the container.
     * They can be files or directories that will be copied to the specified
     * destination path inside the container at startup and are immutable (read-only).
     */
    private List<ResourceMapping> resources = new ArrayList<>();

    /**
     * Strategy for reusing a running container for the dev service across applications or restarts.
     * Only applicable in dev mode.
     */
    private ReuseStrategy reuseStrategy = ReuseStrategy.FRAMEWORK;

    /**
     * Maximum waiting time for the service to start, counted from the container start.
     * Generous default to account for the Langfuse database migrations.
     */
    private Duration startupTimeout = Duration.ofMinutes(5);

    /**
     * Whether the dev service is skipped in test mode, where the application rarely needs a
     * telemetry backend. Set it to false for tests that verify the telemetry the application exports.
     */
    private boolean skipInTests = true;

    /**
     * Files or directories to mount from the host filesystem into the container.
     * They are mounted at the specified destination path inside the container
     * at startup and are mutable (read-write). Changes in either the host
     * or the container will be immediately reflected in the other.
     */
    private List<VolumeMapping> volumes = new ArrayList<>();

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getWorkerImageName() {
        return workerImageName;
    }

    public void setWorkerImageName(String workerImageName) {
        this.workerImageName = workerImageName;
    }

    public String getPostgresImageName() {
        return postgresImageName;
    }

    public void setPostgresImageName(String postgresImageName) {
        this.postgresImageName = postgresImageName;
    }

    public String getClickhouseImageName() {
        return clickhouseImageName;
    }

    public void setClickhouseImageName(String clickhouseImageName) {
        this.clickhouseImageName = clickhouseImageName;
    }

    public String getRedisImageName() {
        return redisImageName;
    }

    public void setRedisImageName(String redisImageName) {
        this.redisImageName = redisImageName;
    }

    public String getFlociImageName() {
        return flociImageName;
    }

    public void setFlociImageName(String flociImageName) {
        this.flociImageName = flociImageName;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public Map<String, String> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    @Override
    public List<String> getNetworkAliases() {
        return networkAliases;
    }

    public void setNetworkAliases(List<String> networkAliases) {
        this.networkAliases = networkAliases;
    }

    @Override
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public List<ResourceMapping> getResources() {
        return resources;
    }

    public void setResources(List<ResourceMapping> resources) {
        this.resources = resources;
    }

    @Override
    public ReuseStrategy getReuseStrategy() {
        return reuseStrategy;
    }

    public void setReuseStrategy(ReuseStrategy reuseStrategy) {
        this.reuseStrategy = reuseStrategy;
    }

    @Override
    public Duration getStartupTimeout() {
        return startupTimeout;
    }

    public void setStartupTimeout(Duration startupTimeout) {
        this.startupTimeout = startupTimeout;
    }

    @Override
    public boolean isSkipInTests() {
        return skipInTests;
    }

    public void setSkipInTests(boolean skipInTests) {
        this.skipInTests = skipInTests;
    }

    public List<VolumeMapping> getVolumes() {
        return volumes;
    }

    public void setVolumes(List<VolumeMapping> volumes) {
        this.volumes = volumes;
    }

}
