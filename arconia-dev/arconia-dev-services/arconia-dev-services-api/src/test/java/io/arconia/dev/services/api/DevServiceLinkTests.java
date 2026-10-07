package io.arconia.dev.services.api;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.api.registration.DevServiceLink;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DevServiceLink}.
 */
class DevServiceLinkTests {

    @Test
    void whenLabelIsEmptyThenThrow() {
        assertThatThrownBy(() -> new DevServiceLink("", "http://localhost:3000"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("label cannot be null or empty");
    }

    @Test
    void whenUrlIsEmptyThenThrow() {
        assertThatThrownBy(() -> new DevServiceLink("Grafana", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("url cannot be null or empty");
    }

    @Test
    void whenAllFieldsAreValidThenCreate() {
        var link = new DevServiceLink("Grafana", "http://localhost:3000");

        assertThat(link.label()).isEqualTo("Grafana");
        assertThat(link.url()).isEqualTo("http://localhost:3000");
    }

}
