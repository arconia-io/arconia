package io.arconia.dev.service.ollama;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OllamaDevServiceProperties}.
 */
class OllamaDevServicePropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(OllamaDevServiceProperties.CONFIG_PREFIX)
                .isEqualTo("arconia.dev.services.ollama");
    }

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        OllamaDevServiceProperties properties = new OllamaDevServiceProperties();

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("ollama/ollama:0.5.13");
        assertThat(properties.isReusable()).isTrue();
    }

    @Test
    void shouldUpdateValues() {
        OllamaDevServiceProperties properties = new OllamaDevServiceProperties();

        properties.setEnabled(true);
        properties.setImageName("ollama/ollama:0.5.12");
        properties.setReusable(false);

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).isEqualTo("ollama/ollama:0.5.12");
        assertThat(properties.isReusable()).isFalse();
    }

}
