package io.arconia.dev.services.pulsar;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.arconia.dev.services.api.config.VolumeMapping;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.arconia.dev.services.api.config.BaseDevServicesProperties;
import io.arconia.dev.services.api.config.ResourceMapping;

/**
 * Properties for the Pulsar Dev Services.
 */
@ConfigurationProperties(prefix = PulsarDevServicesProperties.CONFIG_PREFIX)
public class PulsarDevServicesProperties implements BaseDevServicesProperties {

    public static final String CONFIG_PREFIX = "arconia.dev.services.pulsar";

    /**
     * Whether the dev service is enabled.
     */
    private boolean enabled = true;

    /**
     * Full name of the container image used in the dev service.
     */
    private String imageName = "apachepulsar/pulsar:4.1.3";

    /**
     * Environment variables to set in the service.
     */
    private Map<String,String> environment = new HashMap<>();

    /**
     * Network aliases to assign to the dev service container.
     */
    private List<String> networkAliases = new ArrayList<>();

    /**
     * Fixed port for exposing the Pulsar Broker port to the host.
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
     * Whether the dev service is shared among applications.
     * Only applicable in dev mode.
     */
    private boolean shared = true;

    /**
     * Maximum waiting time for the service to start.
     */
    private Duration startupTimeout = Duration.ofSeconds(30);

    /**
     * Files or directories to mount from the host filesystem into the container.
     * They are mounted at the specified destination path inside the container
     * at startup and are mutable (read-write). Changes in either the host
     * or the container will be immediately reflected in the other.
     */
    private List<VolumeMapping> volumes = new ArrayList<>();

    /**
     * Fixed port for exposing the Pulsar Management Console port to the host.
     * When it's 0 (default), a random available port is assigned dynamically.
     */
    private int managementConsolePort = 0;

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

    public int getManagementConsolePort() {
        return managementConsolePort;
    }

    public void setManagementConsolePort(int managementConsolePort) {
        this.managementConsolePort = managementConsolePort;
    }

}
