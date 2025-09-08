package io.arconia.openinference.observation.autoconfigure;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.Assert;

import io.arconia.core.config.adapter.PropertyAdapter;

/**
 * Provides adapters from OpenInference Environment Variable Specification to Arconia properties.
 *
 * @link <a href="https://arize-ai.github.io/openinference/spec/configuration.html">OpenInference Environment Variable Specification</a>
 */
class OpenInferenceEnvironmentPropertyAdapters {

    /**
     * OpenInference tracing configuration.
     * <p>
     * All environment variables are supported.
     *
     * @link <a href="https://arize-ai.github.io/openinference/spec/configuration.html">Tracing Configuration</a>
     */
    static PropertyAdapter traces(ConfigurableEnvironment environment) {
        Assert.notNull(environment, "environment cannot be null");
        String prefix = OpenInferenceProperties.CONFIG_PREFIX + ".traces";
        return PropertyAdapter.builder(environment)
                .mapBoolean("OPENINFERENCE_HIDE_LLM_INVOCATION_PARAMETERS", prefix + ".hide-llm-invocation-parameters")
                .mapBoolean("OPENINFERENCE_HIDE_INPUTS", prefix + ".hide-inputs")
                .mapBoolean("OPENINFERENCE_HIDE_OUTPUTS", prefix + ".hide-outputs")
                .mapBoolean("OPENINFERENCE_HIDE_INPUT_MESSAGES", prefix + ".hide-input-messages")
                .mapBoolean("OPENINFERENCE_HIDE_OUTPUT_MESSAGES", prefix + ".hide-output-messages")
                .mapBoolean("OPENINFERENCE_HIDE_INPUT_IMAGES", prefix + ".hide-input-images")
                .mapBoolean("OPENINFERENCE_HIDE_INPUT_TEXT", prefix + ".hide-input-text")
                .mapBoolean("OPENINFERENCE_HIDE_PROMPTS", prefix + ".hide-prompts")
                .mapBoolean("OPENINFERENCE_HIDE_OUTPUT_TEXT", prefix + ".hide-output-text")
                .mapBoolean("OPENINFERENCE_HIDE_EMBEDDING_VECTORS", prefix + ".hide-embedding-vectors")
                .mapInteger("OPENINFERENCE_BASE64_IMAGE_MAX_LENGTH", prefix + ".base64-image-max-length")
                .build();
    }

}
