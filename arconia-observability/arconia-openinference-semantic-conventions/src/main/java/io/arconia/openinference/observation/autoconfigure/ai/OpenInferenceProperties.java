package io.arconia.openinference.observation.autoconfigure.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import io.arconia.openinference.observation.instrumentation.ai.OpenInferenceTracingOptions;

/**
 * Configuration properties for the OpenInference instrumentation.
 */
@ConfigurationProperties(prefix = OpenInferenceProperties.CONFIG_PREFIX)
public class OpenInferenceProperties {

    public static final String CONFIG_PREFIX = "arconia.observations.generative-ai.openinference";

    /**
     * Whether to enable the OpenInference instrumentation.
     */
    private boolean enabled = true;

    /**
     * Whether to exclude any non-AI observations from the exported telemetry for the application.
     */
    private boolean exclusive = true;

    /**
     * Options for customizing the OpenInference instrumentation.
     */
    @NestedConfigurationProperty
    private final OpenInferenceTracingOptions traces = new OpenInferenceTracingOptions();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public void setExclusive(boolean exclusive) {
        this.exclusive = exclusive;
    }

    public OpenInferenceTracingOptions getTraces() {
        return traces;
    }

}
