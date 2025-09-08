package io.arconia.openinference.observation.instrumentation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenInferenceTracingOptions}.
 */
class OpenInferenceTracingOptionsTests {

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        OpenInferenceTracingOptions options = new OpenInferenceTracingOptions();

        assertThat(options.getBase64ImageMaxLength()).isEqualTo(32_000);
        assertThat(options.isHideEmbeddingVectors()).isFalse();
        assertThat(options.isHideLlmInvocationParameters()).isFalse();
        assertThat(options.isHideInputs()).isFalse();
        assertThat(options.isHideInputImages()).isFalse();
        assertThat(options.isHideInputMessages()).isFalse();
        assertThat(options.isHideInputText()).isFalse();
        assertThat(options.isHideOutputs()).isFalse();
        assertThat(options.isHideOutputText()).isFalse();
        assertThat(options.isHideOutputMessages()).isFalse();
        assertThat(options.isHidePrompts()).isFalse();
    }

    @Test
    void shouldUpdateValues() {
        OpenInferenceTracingOptions options = new OpenInferenceTracingOptions();
        long newLength = 64_000;

        options.setBase64ImageMaxLength(newLength);
        options.setHideEmbeddingVectors(true);
        options.setHideLlmInvocationParameters(true);
        options.setHideInputs(true);
        options.setHideInputImages(true);
        options.setHideInputMessages(true);
        options.setHideInputText(true);
        options.setHideOutputs(true);
        options.setHideOutputText(true);
        options.setHideOutputMessages(true);
        options.setHidePrompts(true);

        assertThat(options.getBase64ImageMaxLength()).isEqualTo(newLength);
        assertThat(options.isHideEmbeddingVectors()).isTrue();
        assertThat(options.isHideLlmInvocationParameters()).isTrue();
        assertThat(options.isHideInputs()).isTrue();
        assertThat(options.isHideInputImages()).isTrue();
        assertThat(options.isHideInputMessages()).isTrue();
        assertThat(options.isHideInputText()).isTrue();
        assertThat(options.isHideOutputs()).isTrue();
        assertThat(options.isHideOutputText()).isTrue();
        assertThat(options.isHideOutputMessages()).isTrue();
        assertThat(options.isHidePrompts()).isTrue();
    }

}
