package io.arconia.observation.openllmetry.autoconfigure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenLLMetryProperties}.
 */
class OpenLLMetryPropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(OpenLLMetryProperties.CONFIG_PREFIX)
                .isEqualTo("arconia.observations.conventions.openllmetry");
    }

}
