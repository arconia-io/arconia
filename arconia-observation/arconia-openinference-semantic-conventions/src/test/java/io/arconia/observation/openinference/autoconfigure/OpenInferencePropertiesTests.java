package io.arconia.observation.openinference.autoconfigure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenInferenceProperties}.
 */
class OpenInferencePropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(OpenInferenceProperties.CONFIG_PREFIX).isEqualTo("arconia.observations.conventions.openinference");
    }

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        OpenInferenceProperties properties = new OpenInferenceProperties();

        assertThat(properties.isExclusive()).isTrue();
        assertThat(properties.getOptions()).isNotNull();
    }

    @Test
    void shouldUpdateValues() {
        OpenInferenceProperties properties = new OpenInferenceProperties();

        properties.setExclusive(false);
        properties.getOptions().setHideInputMessages(true);

        assertThat(properties.isExclusive()).isFalse();
        assertThat(properties.getOptions().isHideInputMessages()).isTrue();
    }

}
