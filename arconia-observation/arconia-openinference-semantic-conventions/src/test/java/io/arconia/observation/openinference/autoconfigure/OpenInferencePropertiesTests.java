package io.arconia.observation.openinference.autoconfigure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenInferenceProperties}.
 */
class OpenInferencePropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(OpenInferenceProperties.CONFIG_PREFIX)
                .isEqualTo("arconia.observations.conventions.openinference");
    }

}
