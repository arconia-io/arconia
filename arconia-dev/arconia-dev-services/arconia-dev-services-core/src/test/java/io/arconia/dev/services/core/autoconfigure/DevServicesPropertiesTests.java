package io.arconia.dev.services.core.autoconfigure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevServicesProperties}.
 */
class DevServicesPropertiesTests {

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        DevServicesProperties properties = new DevServicesProperties();
        assertThat(properties.isEnabled()).isTrue();
    }

    @Test
    void shouldUpdateValues() {
        DevServicesProperties properties = new DevServicesProperties();
        properties.setEnabled(false);
        assertThat(properties.isEnabled()).isFalse();
    }

}
