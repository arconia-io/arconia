package io.arconia.openinference.observation.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.arconia.openinference.observation.instrumentation.OpenInferenceTracingOptions;

/**
 * Configuration properties for the OpenInference instrumentation.
 */
@ConfigurationProperties(prefix = OpenInferenceProperties.CONFIG_PREFIX)
public class OpenInferenceProperties {

    public static final String CONFIG_PREFIX = "arconia.observability.openinference";

    /**
     * Whether to enable the OpenInference instrumentation.
     */
    private boolean enabled = true;

    /**
     * Whether to exclude any non-AI observations from the exported telemetry for the application.
     */
    private boolean includeOnlyAiObservations = true;

    /**
     * Options for customizing the OpenInference instrumentation.
     */
    private final OpenInferenceTracingOptions traces = new OpenInferenceTracingOptions();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isIncludeOnlyAiObservations() {
        return includeOnlyAiObservations;
    }

    public void setIncludeOnlyAiObservations(boolean includeOnlyAiObservations) {
        this.includeOnlyAiObservations = includeOnlyAiObservations;
    }

    public OpenInferenceTracingOptions getTraces() {
        return traces;
    }

}
