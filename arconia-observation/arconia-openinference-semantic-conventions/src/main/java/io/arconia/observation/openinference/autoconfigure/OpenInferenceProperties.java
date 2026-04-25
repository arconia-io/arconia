package io.arconia.observation.openinference.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.arconia.observation.openinference.instrumentation.OpenInferenceOptions;

/**
 * Configuration properties for the OpenInference instrumentation.
 */
@ConfigurationProperties(prefix = OpenInferenceProperties.CONFIG_PREFIX)
public class OpenInferenceProperties extends OpenInferenceOptions {

    public static final String CONFIG_PREFIX = "arconia.observations.conventions.openinference";

}
