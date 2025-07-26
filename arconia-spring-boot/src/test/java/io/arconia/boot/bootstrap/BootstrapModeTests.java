package io.arconia.boot.bootstrap;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link BootstrapMode}.
 */
class BootstrapModeTests {

    @Test
    void whenValidEnumValue() {
        assertThat(BootstrapMode.isValid("DEV")).isTrue();
        assertThat(BootstrapMode.isValid("TEST")).isTrue();
        assertThat(BootstrapMode.isValid("PROD")).isTrue();
        assertThat(BootstrapMode.isValid("dev")).isTrue();
        assertThat(BootstrapMode.isValid("test")).isTrue();
        assertThat(BootstrapMode.isValid("prod")).isTrue();
        assertThat(BootstrapMode.isValid("\n dev")).isTrue();
        assertThat(BootstrapMode.isValid(" test")).isTrue();
        assertThat(BootstrapMode.isValid("prod   \n")).isTrue();
    }

    @Test
    void whenInvalidEnumValue() {
        assertThat(BootstrapMode.isValid("INVALID")).isFalse();
        assertThat(BootstrapMode.isValid("")).isFalse();
        assertThat(BootstrapMode.isValid(null)).isFalse();
    }

}
