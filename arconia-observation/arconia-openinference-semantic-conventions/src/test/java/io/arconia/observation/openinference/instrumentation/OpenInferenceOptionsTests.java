package io.arconia.observation.openinference.instrumentation;

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
        assertThat(options.isHideChoices()).isFalse();
        assertThat(options.isHideEmbeddingsText()).isFalse();
        assertThat(options.isHideEmbeddingsVectors()).isFalse();
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
        OpenInferenceOptions options = new OpenInferenceOptions();
        long newLength = 64_000;

        options.setBase64ImageMaxLength(newLength);
        options.setHideChoices(true);
        options.setHideEmbeddingsText(true);
        options.setHideEmbeddingsVectors(true);
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
        assertThat(options.isHideChoices()).isTrue();
        assertThat(options.isHideEmbeddingsText()).isTrue();
        assertThat(options.isHideEmbeddingsVectors()).isTrue();
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
