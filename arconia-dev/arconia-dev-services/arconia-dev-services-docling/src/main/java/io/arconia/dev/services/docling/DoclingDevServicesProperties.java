package io.arconia.dev.services.docling;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

import io.arconia.dev.services.api.config.ResourceMapping;
import io.arconia.dev.services.api.config.SharedDevServicesProperties;
import io.arconia.dev.services.api.config.VolumeMapping;

/**
 * Properties for the Docling Dev Services.
 */
@ConfigurationProperties(prefix = DoclingDevServicesProperties.CONFIG_PREFIX)
public class DoclingDevServicesProperties implements SharedDevServicesProperties {

    public static final String CONFIG_PREFIX = "arconia.dev.services.docling";

    /**
     * Whether the dev service is enabled.
     */
    private boolean enabled = true;

    /**
     * Full name of the container image used in the dev service.
     */
    private String imageName = "ghcr.io/docling-project/docling-serve:v1.30.0";

    /**
     * Environment variables to set in the service.
     */
    private Map<String,String> environment = new HashMap<>();

    /**
     * Network aliases to assign to the dev service container.
     */
    private List<String> networkAliases = new ArrayList<>();

    /**
     * Fixed port for exposing the Docling Serve HTTP port to the host.
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
     * Whether to enable the Docling UI when in dev mode.
     */
    private boolean enableUi = true;

    /**
     * API key to be used for authenticating requests to the Docling Serve API.
     */
    @Nullable
    private String apiKey;

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

    public boolean isEnableUi() {
        return enableUi;
    }

    public void setEnableUi(boolean enableUi) {
        this.enableUi = enableUi;
    }

    @Nullable
    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(@Nullable String apiKey) {
        this.apiKey = apiKey;
    }

}
