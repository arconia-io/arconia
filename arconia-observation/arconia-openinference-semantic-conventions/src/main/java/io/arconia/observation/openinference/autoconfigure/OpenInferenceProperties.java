package io.arconia.observation.openinference.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import io.arconia.observation.openinference.instrumentation.OpenInferenceOptions;

/**
 * Configuration properties for the OpenInference instrumentation.
 */
@ConfigurationProperties(prefix = OpenInferenceProperties.CONFIG_PREFIX)
public class OpenInferenceProperties {

    public static final String CONFIG_PREFIX = "arconia.observations.conventions.openinference";

    /**
     * Whether to exclude any observation not being of the OpenInference kind.
     */
    private boolean exclusive = true;

    /**
     * Options for customizing the OpenInference instrumentation.
     */
    @NestedConfigurationProperty
    private final OpenInferenceOptions options = new OpenInferenceOptions();

    public boolean isExclusive() {
        return exclusive;
    }

    public void setExclusive(boolean exclusive) {
        this.exclusive = exclusive;
    }

    public OpenInferenceOptions getOptions() {
        return options;
    }

}
