package io.arconia.opentelemetry.autoconfigure.sdk.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.opentelemetry.sdk.resources.Resource;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor.FilterResourceContributor;

/**
 * Configuration properties for OpenTelemetry {@link Resource},
 * a set of attributes defining the telemetry source.
 */
@ConfigurationProperties(prefix = OpenTelemetryResourceProperties.CONFIG_PREFIX)
public class OpenTelemetryResourceProperties {

    public static final String CONFIG_PREFIX = "arconia.opentelemetry.resource";

    /**
     * Additional attributes to include in the resource.
     */
    private final Map<String, String> attributes = new HashMap<>();

    /**
     * Configuration for the {@link FilterResourceContributor}.
     */
    private final Filter filter = new Filter();

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public Filter getFilter() {
        return filter;
    }

    public static class Filter {

        /**
         * Keys for attributes to exclude from the resource.
         */
        private final List<String> disabledKeys = new ArrayList<>();

        public List<String> getDisabledKeys() {
            return disabledKeys;
        }

    }

}
