package io.arconia.dev.services.openlit;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.arconia.dev.services.api.config.SharedDevServicesProperties;
import io.arconia.dev.services.api.config.ResourceMapping;
import io.arconia.dev.services.api.config.VolumeMapping;

/**
 * Properties for the OpenLit Dev Services.
 */
@ConfigurationProperties(prefix = OpenLitDevServicesProperties.CONFIG_PREFIX)
public class OpenLitDevServicesProperties implements SharedDevServicesProperties {

    public static final String CONFIG_PREFIX = "arconia.dev.services.openlit";

    /**
     * Whether the dev service is enabled.
     */
    private boolean enabled = true;

    /**
     * Full name of the container image used for OpenLit.
     */
    private String imageName = "ghcr.io/openlit/openlit:1.22.0";

    /**
     * Full name of the container image used for the internal ClickHouse instance.
     */
    private String clickhouseImageName = "clickhouse/clickhouse-server:26.5-distroless";

    /**
     * Environment variables to set in the OpenLit service.
     */
    private Map<String, String> environment = new HashMap<>();

    /**
     * Network aliases to assign to the OpenLit container.
     */
    private List<String> networkAliases = new ArrayList<>();

    /**
     * Fixed port for exposing the OpenLit UI to the host.
     * When it's 0 (default), a random available port is assigned dynamically.
     */
    private int port = 0;

    /**
     * Fixed port for exposing the OTLP gRPC port to the host.
     * When it's 0 (default), a random available port is assigned dynamically.
     */
    private int otlpGrpcPort = 0;

    /**
     * Fixed port for exposing the OTLP HTTP port to the host.
     * When it's 0 (default), a random available port is assigned dynamically.
     */
    private int otlpHttpPort = 0;

    /**
     * Resources from the classpath or host filesystem to copy into the container.
     */
    private List<ResourceMapping> resources = new ArrayList<>();

    /**
     * Whether the container used in the dev service is reused across multiple
     * applications and application restarts, relying on the Testcontainers
     * reusable containers feature. It requires enabling the feature
     * in the `~/.testcontainers.properties` file. Reused containers
     * are not stopped automatically and must be cleaned up manually.
     * Only applicable in dev mode.
     */
    private boolean reuse = false;

    /**
     * Whether the dev service is shared among applications running simultaneously.
     * A shared dev service is discoverable by other applications, and the application
     * connects to an existing shared dev service if available instead of starting a new one.
     * Container reuse takes precedence: when the `reuse` property is enabled,
     * sharing is disabled. Only applicable in dev mode.
     */
    private boolean shared = true;

    /**
     * Maximum waiting time for the service to start.
     * Generous default to account for ClickHouse initialization time.
     */
    private Duration startupTimeout = Duration.ofMinutes(2);

    /**
     * Files or directories to mount from the host filesystem into the container.
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

    public String getClickhouseImageName() {
        return clickhouseImageName;
    }

    public void setClickhouseImageName(String clickhouseImageName) {
        this.clickhouseImageName = clickhouseImageName;
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

    public int getOtlpGrpcPort() {
        return otlpGrpcPort;
    }

    public void setOtlpGrpcPort(int otlpGrpcPort) {
        this.otlpGrpcPort = otlpGrpcPort;
    }

    public int getOtlpHttpPort() {
        return otlpHttpPort;
    }

    public void setOtlpHttpPort(int otlpHttpPort) {
        this.otlpHttpPort = otlpHttpPort;
    }

    @Override
    public List<ResourceMapping> getResources() {
        return resources;
    }

    public void setResources(List<ResourceMapping> resources) {
        this.resources = resources;
    }

    @Override
    public boolean isReuse() {
        return reuse;
    }

    public void setReuse(boolean reuse) {
        this.reuse = reuse;
    }

    @Override
    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    @Override
    public Duration getStartupTimeout() {
        return startupTimeout;
    }

    public void setStartupTimeout(Duration startupTimeout) {
        this.startupTimeout = startupTimeout;
    }

    @Override
    public List<VolumeMapping> getVolumes() {
        return volumes;
    }

    public void setVolumes(List<VolumeMapping> volumes) {
        this.volumes = volumes;
    }

}
