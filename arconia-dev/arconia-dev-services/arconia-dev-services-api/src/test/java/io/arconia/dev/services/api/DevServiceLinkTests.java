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
    void whenIdIsNullThenThrow() {
        assertThatThrownBy(() -> DevServiceLink.builder().id(null).label("Grafana").url("http://localhost:3000").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id cannot be null or empty");
    }

    @Test
    void whenIdIsEmptyThenThrow() {
        assertThatThrownBy(() -> DevServiceLink.builder().id("").label("Grafana").url("http://localhost:3000").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id cannot be null or empty");
    }

    @Test
    void whenLabelIsNullThenThrow() {
        assertThatThrownBy(() -> DevServiceLink.builder().id("grafana").label(null).url("http://localhost:3000").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("label cannot be null or empty");
    }

    @Test
    void whenLabelIsEmptyThenThrow() {
        assertThatThrownBy(() -> DevServiceLink.builder().id("grafana").label("").url("http://localhost:3000").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("label cannot be null or empty");
    }

    @Test
    void whenUrlIsNullThenThrow() {
        assertThatThrownBy(() -> DevServiceLink.builder().id("grafana").label("Grafana").url(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("url cannot be null or empty");
    }

    @Test
    void whenUrlIsEmptyThenThrow() {
        assertThatThrownBy(() -> DevServiceLink.builder().id("grafana").label("Grafana").url("").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("url cannot be null or empty");
    }

    @Test
    void whenAllFieldsAreValidThenCreate() {
        var link = DevServiceLink.builder()
                .id("grafana")
                .label("Grafana")
                .url("http://localhost:3000")
                .build();

        assertThat(link.id()).isEqualTo("grafana");
        assertThat(link.label()).isEqualTo("Grafana");
        assertThat(link.url()).isEqualTo("http://localhost:3000");
    }

    @Test
    void whenCreatedViaCanonicalConstructorThenCreate() {
        var link = new DevServiceLink("otlp", "OTLP/HTTP", "http://localhost:4318");

        assertThat(link.id()).isEqualTo("otlp");
        assertThat(link.label()).isEqualTo("OTLP/HTTP");
        assertThat(link.url()).isEqualTo("http://localhost:4318");
    }

}
