package io.arconia.observation.openinference.ai.autoconfigure;

import org.springframework.util.Assert;

/**
 * Provides adapters from OpenInference Environment Variable Specification to Arconia properties.
 *
 * @link <a href="https://arize-ai.github.io/openinference/spec/configuration.html">OpenInference Environment Variable Specification</a>
 */
class OpenInferenceEnvironmentPropertyAdapters {

    /**
     * OpenInference tracing configuration.
     * <p>
     * All environment variables are supported except:
     * <ul>
     *   <li>{@code OPENINFERENCE_HIDE_PROMPTS}</li>
     *   <li>{@code OPENINFERENCE_HIDE_CHOICES}</li>
     *   <li>{@code OPENINFERENCE_HIDE_RETRIEVAL_DOCUMENTS}</li>
     *   <li>{@code OPENINFERENCE_BLOB_UPLOADER}</li>
     * </ul>
     * <p>
     * @link <a href="https://arize-ai.github.io/openinference/spec/configuration.html">Tracing Configuration</a>
     */
    static OpenInferenceEnvironmentPropertyAdapter traces(OpenInferenceEnvironmentPropertyAdapter adapter) {
        Assert.notNull(adapter, "adapter cannot be null");
        String prefix = OpenInferenceProperties.CONFIG_PREFIX;
        return adapter
                .mapBoolean("OPENINFERENCE_HIDE_LLM_INVOCATION_PARAMETERS", prefix + ".hide-llm-invocation-parameters")
                .mapBoolean("OPENINFERENCE_HIDE_LLM_TOOLS", prefix + ".hide-llm-tools")
                .mapBoolean("OPENINFERENCE_HIDE_INPUTS", prefix + ".hide-inputs")
                .mapBoolean("OPENINFERENCE_HIDE_OUTPUTS", prefix + ".hide-outputs")
                .mapBoolean("OPENINFERENCE_HIDE_INPUT_MESSAGES", prefix + ".hide-input-messages")
                .mapBoolean("OPENINFERENCE_HIDE_OUTPUT_MESSAGES", prefix + ".hide-output-messages")
                .mapBoolean("OPENINFERENCE_HIDE_INPUT_IMAGES", prefix + ".hide-input-images")
                .mapBoolean("OPENINFERENCE_HIDE_INPUT_TEXT", prefix + ".hide-input-text")
                .mapBoolean("OPENINFERENCE_HIDE_OUTPUT_TEXT", prefix + ".hide-output-text")
                .mapBoolean("OPENINFERENCE_HIDE_EMBEDDINGS_TEXT", prefix + ".hide-embeddings-text")
                .mapBoolean("OPENINFERENCE_HIDE_EMBEDDINGS_VECTORS", prefix + ".hide-embeddings-vectors")
                .mapInteger("OPENINFERENCE_BASE64_IMAGE_MAX_LENGTH", prefix + ".base64-image-max-length");
    }

}
