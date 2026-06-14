package io.arconia.dev.services.openlit;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.arconia.dev.services.api.config.BaseDevServicesProperties;
import io.arconia.dev.services.api.config.ResourceMapping;
import io.arconia.dev.services.api.config.VolumeMapping;

/**
 * Properties for the OpenLit Dev Services.
 */
@ConfigurationProperties(prefix = OpenLitDevServicesProperties.CONFIG_PREFIX)
public class OpenLitDevServicesProperties implements BaseDevServicesProperties {

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
     * Whether the dev service is shared among applications.
     * Only applicable in dev mode.
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

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getImageName() {
        return imageName;
    }

    @Override
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

    @Override
    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    @Override
    public List<String> getNetworkAliases() {
        return networkAliases;
    }

    @Override
    public void setNetworkAliases(List<String> networkAliases) {
        this.networkAliases = networkAliases;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
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

    @Override
    public void setResources(List<ResourceMapping> resources) {
        this.resources = resources;
    }

    @Override
    public boolean isShared() {
        return shared;
    }

    @Override
    public void setShared(boolean shared) {
        this.shared = shared;
    }

    @Override
    public Duration getStartupTimeout() {
        return startupTimeout;
    }

    @Override
    public void setStartupTimeout(Duration startupTimeout) {
        this.startupTimeout = startupTimeout;
    }

    @Override
    public List<VolumeMapping> getVolumes() {
        return volumes;
    }

    @Override
    public void setVolumes(List<VolumeMapping> volumes) {
        this.volumes = volumes;
    }

}
