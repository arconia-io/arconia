package io.arconia.opentelemetry.autoconfigure.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.opentelemetry.sdk.resources.Resource;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for OpenTelemetry {@link Resource},
 * a set of attributes defining the telemetry source.
 */
@ConfigurationProperties(prefix = OpenTelemetryResourceProperties.CONFIG_PREFIX)
public class OpenTelemetryResourceProperties {

    public static final String CONFIG_PREFIX = "arconia.opentelemetry.resource";

    /**
     * Providers of attributes to include in the resource.
     */
    private final Providers providers = new Providers();

    /**
     * Additional attributes to include in the resource.
     */
    private final Map<String, String> attributes = new HashMap<>();

    /**
     * Keys for attributes to exclude from the resource.
     */
    private final List<String> disabledKeys = new ArrayList<>();

    public Providers getProviders() {
        return providers;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public List<String> getDisabledKeys() {
        return disabledKeys;
    }

    public static class Providers {

        /**
         * Whether to include environment attributes in the resource, such as "service.name" and "service.group".
         */
        private boolean environment = true;

        /**
         * Whether to include build attributes in the resource, such as "service.version".
         */
        private boolean build = true;

        public boolean isEnvironment() {
            return environment;
        }

        public void setEnvironment(boolean environment) {
            this.environment = environment;
        }

        public boolean isBuild() {
            return build;
        }

        public void setBuild(boolean build) {
            this.build = build;
        }

    }

}
