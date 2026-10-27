package io.arconia.opentelemetry.autoconfigure.resource;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for OpenTelemetry Resource.
 */
@ConfigurationProperties(prefix = OpenTelemetryResourceProperties.CONFIG_PREFIX)
public class OpenTelemetryResourceProperties {

    public static final String CONFIG_PREFIX = "arconia.otel.resource";

    /**
     * Name identifying the service.
     */
    @Nullable
    private String serviceName;

    /**
     * Name of the deployment environment (also known as deployment tier), such as 'production', 'staging',
     * 'test', or 'development'.
     */
    @Nullable
    private String deploymentEnvironmentName;

    /**
     * Additional attributes to include in the resource.
     */
    private final Map<String, String> attributes = new HashMap<>();

    /**
     * Whether resource attributes having keys starting with the specified name should be enabled.
     * The longest match wins, comparing dot-separated segments. The key {@code all} can be used to
     * enable/disable all the attributes not matched by a more specific name.
     */
    private final Map<String, Boolean> enable = new LinkedHashMap<>();

    @Nullable
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Nullable
    public String getDeploymentEnvironmentName() {
        return deploymentEnvironmentName;
    }

    public void setDeploymentEnvironmentName(String deploymentEnvironmentName) {
        this.deploymentEnvironmentName = deploymentEnvironmentName;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public Map<String, Boolean> getEnable() {
        return enable;
    }

}
