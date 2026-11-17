package io.arconia.opentelemetry.autoconfigure.observations;

import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link HttpServerObservationsProperties}.
 */
class HttpServerObservationsPropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(HttpServerObservationsProperties.CONFIG_PREFIX).isEqualTo("arconia.observations.http.server");
    }

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        HttpServerObservationsProperties properties = new HttpServerObservationsProperties();

        assertThat(properties.getIgnorePaths()).containsExactlyInAnyOrder("/actuator/health/**", "/livez", "/readyz");
        assertThat(properties.getAdditionalIgnorePaths()).isEmpty();
    }

    @Test
    void shouldUpdateValues() {
        HttpServerObservationsProperties properties = new HttpServerObservationsProperties();

        properties.setIgnorePaths(Set.of("/health"));
        properties.setAdditionalIgnorePaths(Set.of("/internal/**"));

        assertThat(properties.getIgnorePaths()).containsExactly("/health");
        assertThat(properties.getAdditionalIgnorePaths()).containsExactly("/internal/**");
    }

}
