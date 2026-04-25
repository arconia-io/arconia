package io.arconia.observation.autoconfigure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ObservationProperties}.
 */
class ObservationPropertiesTests {

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        ObservationProperties properties = new ObservationProperties();

        assertThat(properties.getConventions()).isNotNull();
        assertThat(properties.getConventions().getType()).isNull();
    }

    @Test
    void shouldUpdateConventionsType() {
        ObservationProperties properties = new ObservationProperties();

        properties.getConventions().setType("openinference");

        assertThat(properties.getConventions().getType()).isEqualTo("openinference");
    }

}
