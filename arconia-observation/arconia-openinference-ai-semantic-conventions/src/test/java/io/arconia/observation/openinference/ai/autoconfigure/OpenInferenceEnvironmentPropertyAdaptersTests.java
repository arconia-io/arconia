package io.arconia.observation.openinference.ai.autoconfigure;

import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link OpenInferenceEnvironmentPropertyAdapters}.
 */
@ExtendWith(OutputCaptureExtension.class)
class OpenInferenceEnvironmentPropertyAdaptersTests {

    @Test
    void tracesShouldMapProperties() {
        var environment = new MockEnvironment()
                .withProperty("OPENINFERENCE_HIDE_LLM_INVOCATION_PARAMETERS", "true")
                .withProperty("OPENINFERENCE_HIDE_LLM_TOOLS", "true")
                .withProperty("OPENINFERENCE_HIDE_INPUTS", "true")
                .withProperty("OPENINFERENCE_HIDE_OUTPUTS", "true")
                .withProperty("OPENINFERENCE_HIDE_INPUT_MESSAGES", "true")
                .withProperty("OPENINFERENCE_HIDE_OUTPUT_MESSAGES", "true")
                .withProperty("OPENINFERENCE_HIDE_INPUT_IMAGES", "true")
                .withProperty("OPENINFERENCE_HIDE_INPUT_TEXT", "true")
                .withProperty("OPENINFERENCE_HIDE_OUTPUT_TEXT", "true")
                .withProperty("OPENINFERENCE_HIDE_EMBEDDINGS_TEXT", "true")
                .withProperty("OPENINFERENCE_HIDE_EMBEDDINGS_VECTORS", "true")
                .withProperty("OPENINFERENCE_BASE64_IMAGE_MAX_LENGTH", "64000");

        var adapter = OpenInferenceEnvironmentPropertyAdapters.traces(adapter(environment));

        String prefix = OpenInferenceProperties.CONFIG_PREFIX;
        assertThat(adapter.getArconiaProperties().get(prefix + ".hide-llm-invocation-parameters")).isEqualTo(true);
        assertThat(adapter.getArconiaProperties().get(prefix + ".hide-llm-tools")).isEqualTo(true);
        assertThat(adapter.getArconiaProperties().get(prefix + ".hide-inputs")).isEqualTo(true);
        assertThat(adapter.getArconiaProperties().get(prefix + ".hide-outputs")).isEqualTo(true);
        assertThat(adapter.getArconiaProperties().get(prefix + ".hide-input-messages")).isEqualTo(true);
        assertThat(adapter.getArconiaProperties().get(prefix + ".hide-output-messages")).isEqualTo(true);
        assertThat(adapter.getArconiaProperties().get(prefix + ".hide-input-images")).isEqualTo(true);
        assertThat(adapter.getArconiaProperties().get(prefix + ".hide-input-text")).isEqualTo(true);
        assertThat(adapter.getArconiaProperties().get(prefix + ".hide-output-text")).isEqualTo(true);
        assertThat(adapter.getArconiaProperties().get(prefix + ".hide-embeddings-text")).isEqualTo(true);
        assertThat(adapter.getArconiaProperties().get(prefix + ".hide-embeddings-vectors")).isEqualTo(true);
        assertThat(adapter.getArconiaProperties().get(prefix + ".base64-image-max-length")).isEqualTo(64000);
    }

    @Test
    void tracesShouldThrowExceptionWhenAdapterIsNull() {
        assertThatThrownBy(() -> OpenInferenceEnvironmentPropertyAdapters.traces(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("adapter cannot be null");
    }

    @ParameterizedTest
    @ValueSource(strings = { "yes", "1", "on" })
    void tracesShouldSkipInvalidBooleans(String value, CapturedOutput output) {
        var environment = new MockEnvironment().withProperty("OPENINFERENCE_HIDE_INPUTS", value);

        var adapter = OpenInferenceEnvironmentPropertyAdapters.traces(adapter(environment));

        assertThat(adapter.getArconiaProperties()).isEmpty();
        assertThat(output).contains("Unsupported value for OPENINFERENCE_HIDE_INPUTS: " + value);
    }

    @Test
    void tracesShouldSkipInvalidIntegers(CapturedOutput output) {
        var environment = new MockEnvironment().withProperty("OPENINFERENCE_BASE64_IMAGE_MAX_LENGTH", "not-a-number");

        var adapter = OpenInferenceEnvironmentPropertyAdapters.traces(adapter(environment));

        assertThat(adapter.getArconiaProperties()).isEmpty();
        assertThat(output).contains("Unsupported value for OPENINFERENCE_BASE64_IMAGE_MAX_LENGTH: not-a-number");
    }

    private static OpenInferenceEnvironmentPropertyAdapter adapter(MockEnvironment environment) {
        return new OpenInferenceEnvironmentPropertyAdapter(environment, LogFactory.getLog(OpenInferenceEnvironmentPropertyAdaptersTests.class));
    }

}
