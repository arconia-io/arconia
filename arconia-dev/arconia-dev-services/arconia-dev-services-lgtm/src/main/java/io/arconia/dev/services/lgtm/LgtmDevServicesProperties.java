package io.arconia.dev.services.lgtm;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.arconia.dev.services.api.config.BaseDevServicesProperties;

/**
 * Properties for the Grafana LGTM Dev Services.
 */
@ConfigurationProperties(prefix = LgtmDevServicesProperties.CONFIG_PREFIX)
public class LgtmDevServicesProperties implements BaseDevServicesProperties {

    public static final String CONFIG_PREFIX = "arconia.dev.services.lgtm";

    /**
     * Whether the dev service is enabled.
     */
    private boolean enabled = true;

    /**
     * Full name of the container image used in the dev service.
     */
    private String imageName = "grafana/otel-lgtm:0.13.0";

    /**
     * Environment variables to set in the service.
     */
    private Map<String,String> environment = new HashMap<>();

    /**
     * Network aliases to assign to the dev service container.
     */
    private List<String> networkAliases = new ArrayList<>();

    /**
     * Fixed port for exposing the Grafana port to the host.
     * When it's 0 (default), a random available port is assigned dynamically.
     */
    private int port = 0;

    /**
     * Whether the dev service is shared among applications.
     * Only applicable in dev mode.
     */
    private boolean shared = true;

    /**
     * Maximum waiting time for the service to start.
     */
    private Duration startupTimeout = Duration.ofMinutes(2);

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

}
