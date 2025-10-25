package io.arconia.openinference.observation.autoconfigure.ai;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link OpenInferenceEnvironmentPropertyAdapters}.
 */
class OpenInferenceEnvironmentPropertyAdaptersTests {

    @Test
    void tracesShouldMapProperties() {
        var environment = new MockEnvironment()
                .withProperty("OPENINFERENCE_HIDE_LLM_INVOCATION_PARAMETERS", "true")
                .withProperty("OPENINFERENCE_HIDE_INPUTS", "true")
                .withProperty("OPENINFERENCE_HIDE_OUTPUTS", "true")
                .withProperty("OPENINFERENCE_HIDE_INPUT_MESSAGES", "true")
                .withProperty("OPENINFERENCE_HIDE_OUTPUT_MESSAGES", "true")
                .withProperty("OPENINFERENCE_HIDE_INPUT_IMAGES", "true")
                .withProperty("OPENINFERENCE_HIDE_INPUT_TEXT", "true")
                .withProperty("OPENINFERENCE_HIDE_PROMPTS", "true")
                .withProperty("OPENINFERENCE_HIDE_OUTPUT_TEXT", "true")
                .withProperty("OPENINFERENCE_HIDE_EMBEDDING_VECTORS", "true")
                .withProperty("OPENINFERENCE_BASE64_IMAGE_MAX_LENGTH", "64000");

        var adapter = OpenInferenceEnvironmentPropertyAdapters.traces(environment);

        String prefix = OpenInferenceProperties.CONFIG_PREFIX + ".traces";
        assertThat(adapter.getArconiaProperties().get(prefix + ".hide-llm-invocation-parameters")).isEqualTo(true);
        assertThat(adapter.getArconiaProperties().get(prefix + ".hide-inputs")).isEqualTo(true);
        assertThat(adapter.getArconiaProperties().get(prefix + ".hide-outputs")).isEqualTo(true);
        assertThat(adapter.getArconiaProperties().get(prefix + ".hide-input-messages")).isEqualTo(true);
        assertThat(adapter.getArconiaProperties().get(prefix + ".hide-output-messages")).isEqualTo(true);
        assertThat(adapter.getArconiaProperties().get(prefix + ".hide-input-images")).isEqualTo(true);
        assertThat(adapter.getArconiaProperties().get(prefix + ".hide-input-text")).isEqualTo(true);
        assertThat(adapter.getArconiaProperties().get(prefix + ".hide-prompts")).isEqualTo(true);
        assertThat(adapter.getArconiaProperties().get(prefix + ".hide-output-text")).isEqualTo(true);
        assertThat(adapter.getArconiaProperties().get(prefix + ".hide-embedding-vectors")).isEqualTo(true);
        assertThat(adapter.getArconiaProperties().get(prefix + ".base64-image-max-length")).isEqualTo(64000);
    }

    @Test
    void tracesShouldHandleInvalidBooleanValues() {
        var environment = new MockEnvironment()
                .withProperty("OPENINFERENCE_HIDE_LLM_INVOCATION_PARAMETERS", "not-a-boolean");

        var adapter = OpenInferenceEnvironmentPropertyAdapters.traces(environment);

        String prefix = OpenInferenceProperties.CONFIG_PREFIX + ".traces";
        assertThat(adapter.getArconiaProperties().containsKey(prefix + ".hide-llm-invocation-parameters")).isTrue();
        assertThat(adapter.getArconiaProperties().get(prefix + ".hide-llm-invocation-parameters")).isEqualTo(false);
    }

    @Test
    void tracesShouldHandleInvalidIntegerValues() {
        var environment = new MockEnvironment()
                .withProperty("OPENINFERENCE_BASE64_IMAGE_MAX_LENGTH", "not-a-number");

        var adapter = OpenInferenceEnvironmentPropertyAdapters.traces(environment);

        String prefix = OpenInferenceProperties.CONFIG_PREFIX + ".traces";
        assertThat(adapter.getArconiaProperties().containsKey(prefix + ".base64-image-max-length")).isFalse();
    }

    @Test
    void tracesShouldThrowExceptionWhenEnvironmentIsNull() {
        assertThatThrownBy(() -> OpenInferenceEnvironmentPropertyAdapters.traces(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("environment cannot be null");
    }

}
