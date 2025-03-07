package io.arconia.opentelemetry.autoconfigure.sdk.resource;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import io.opentelemetry.sdk.resources.Resource;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for OpenTelemetry {@link Resource},
 * a set of attributes defining the telemetry source.
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
     * Additional attributes to include in the resource.
     */
    private final Map<String, String> attributes = new HashMap<>();

    /**
     * Whether resource attributes having keys starting with the specified name
     * should be enabled. The longest match wins.
     */
    private final Map<String, Boolean> enable = new LinkedHashMap<>();

    @Nullable
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public Map<String, Boolean> getEnable() {
        return enable;
    }

}
