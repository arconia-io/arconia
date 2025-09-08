package io.arconia.openinference.observation.autoconfigure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenInferenceProperties}.
 */
class OpenInferencePropertiesTests {

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        OpenInferenceProperties properties = new OpenInferenceProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getTraces()).isNotNull();
    }

    @Test
    void shouldUpdateValues() {
        OpenInferenceProperties properties = new OpenInferenceProperties();

        properties.setEnabled(false);
        properties.getTraces().setHideInputMessages(true);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getTraces().isHideInputMessages()).isTrue();
    }

}
