package io.arconia.observation.openinference.ai.autoconfigure;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

import io.arconia.observation.openinference.ai.instrumentation.OpenInferenceOptions;

/**
 * Configuration properties for the OpenInference instrumentation.
 */
@ConfigurationProperties(prefix = OpenInferenceProperties.CONFIG_PREFIX)
public class OpenInferenceProperties extends OpenInferenceOptions {

    public static final String CONFIG_PREFIX = "arconia.observations.conventions.openinference.ai";

    /**
     * Whether to enable OpenInference semantic conventions.
     */
    private boolean enabled = true;

    /**
     * Name of the project in the OpenInference backend where to send the telemetry data.
     */
    @Nullable
    private String projectName;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public @Nullable String getProjectName() {
        return projectName;
    }

    public void setProjectName(@Nullable String projectName) {
        this.projectName = projectName;
    }

}
