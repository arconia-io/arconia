package io.arconia.dev.services.elasticsearch;

import io.arconia.dev.services.api.config.BaseDevServicesProperties;

import io.arconia.dev.services.api.config.ResourceMapping;
import io.arconia.dev.services.api.config.VolumeMapping;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = ElasticsearchDevServicesProperties.CONFIG_PREFIX)
public class ElasticsearchDevServicesProperties implements BaseDevServicesProperties {
    public static final String CONFIG_PREFIX = "arconia.dev.services.elasticsearch";

    /**
     * Whether the dev service is enabled.
     */
    private boolean enabled = true;

    /**
     * Fixed port for exposing the MariaDB database port to the host.
     * When it's 0 (default), a random available port is assigned dynamically.
     */
    private int port = 0;

    /**
     * Whether the dev service is shared among applications.
     * Only applicable in dev mode.
     */
    private boolean shared = false;

    /**
     * Full name of the container image used in the dev service.
     */
    private String imageName = "elasticsearch:9.4.1";

    /**
     * Environment variables to set in the service.
     */
    private Map<String, String> environment = new HashMap<>();

    /**
     * Network aliases to assign to the dev service container.
     */
    private List<String> networkAliases = new ArrayList<>();

    /**
     * Resources from the classpath or host filesystem to copy into the container.
     * They can be files or directories that will be copied to the specified
     * destination path inside the container at startup and are immutable (read-only).
     */
    private List<ResourceMapping> resources = new ArrayList<>();

    /**
     * Files or directories to mount from the host filesystem into the container.
     * They are mounted at the specified destination path inside the container
     * at startup and are mutable (read-write). Changes in either the host
     * or the container will be immediately reflected in the other.
     */
    private List<VolumeMapping> volumes = new ArrayList<>();

    /**
     * Maximum waiting time for the service to start.
     */
    private Duration startupTimeout = Duration.ofSeconds(30);

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public String getImageName() {
        return this.imageName;
    }

    @Override
    public Map<String, String> getEnvironment() {
        return this.environment;
    }

    @Override
    public List<String> getNetworkAliases() {
        return this.networkAliases;
    }

    @Override
    public int getPort() {
        return this.port;
    }

    @Override
    public List<ResourceMapping> getResources() {
        return this.resources;
    }

    @Override
    public boolean isShared() {
        return this.shared;
    }

    @Override
    public Duration getStartupTimeout() {
        return this.startupTimeout;
    }

    @Override
    public List<VolumeMapping> getVolumes() {
        return this.volumes;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    @Override
    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    @Override
    public void setNetworkAliases(List<String> networkAliases) {
        this.networkAliases = networkAliases;
    }

    @Override
    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public void setResources(List<ResourceMapping> resources) {
        this.resources = resources;
    }

    @Override
    public void setShared(boolean shared) {
        this.shared = shared;
    }

    @Override
    public void setStartupTimeout(Duration startupTimeout) {
        this.startupTimeout = startupTimeout;
    }

    @Override
    public void setVolumes(List<VolumeMapping> volumes) {
        this.volumes = volumes;
    }
}
