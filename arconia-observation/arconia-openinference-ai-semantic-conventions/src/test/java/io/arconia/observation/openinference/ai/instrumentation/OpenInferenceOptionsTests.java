package io.arconia.observation.openinference.ai.instrumentation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenInferenceOptions}.
 */
class OpenInferenceOptionsTests {

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        OpenInferenceOptions options = new OpenInferenceOptions();

        assertThat(options.getBase64ImageMaxLength()).isEqualTo(32_000);
        assertThat(options.isHideEmbeddingsText()).isFalse();
        assertThat(options.isHideEmbeddingsVectors()).isFalse();
        assertThat(options.isHideLlmInvocationParameters()).isFalse();
        assertThat(options.isHideLlmTools()).isFalse();
        assertThat(options.isHideInputs()).isFalse();
        assertThat(options.isHideInputImages()).isFalse();
        assertThat(options.isHideInputMessages()).isFalse();
        assertThat(options.isHideInputText()).isFalse();
        assertThat(options.isHideOutputs()).isFalse();
        assertThat(options.isHideOutputText()).isFalse();
        assertThat(options.isHideOutputMessages()).isFalse();
    }

}
