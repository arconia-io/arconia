package io.arconia.dev.services.docling;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

import io.arconia.dev.services.core.config.DevServicesProperties;

/**
 * Properties for the Docling Dev Services.
 */
@ConfigurationProperties(prefix = "arconia.dev.services.docling")
public class DoclingDevServicesProperties implements DevServicesProperties {

    /**
     * Whether the dev service is enabled.
     */
    private boolean enabled = true;

    /**
     * Full name of the container image used in the dev service.
     */
    private String imageName = "ghcr.io/docling-project/docling-serve:v1.9.0";

    /**
     * Port for the XXX. When it's 0 (default value), a random port is assigned by Testcontainers.
     */
    private int port = 0;

    /**
     * Environment variables to set in the service.
     */
    private Map<String,String> environment = new HashMap<>();

    /**
     * When the dev service is shared across applications.
     */
    private Shared shared = Shared.DEV_MODE;

    /**
     * Maximum waiting time for the service to start.
     */
    private Duration startupTimeout = Duration.ofMinutes(2);

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
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public Map<String, String> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    @Override
    public Shared getShared() {
        return shared;
    }

    public void setShared(Shared shared) {
        this.shared = shared;
    }

    @Override
    public Duration getStartupTimeout() {
        return startupTimeout;
    }

    public void setStartupTimeout(Duration startupTimeout) {
        this.startupTimeout = startupTimeout;
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
