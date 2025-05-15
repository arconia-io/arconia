package io.arconia.dev.services.ollama;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.core.config.DevServicesProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OllamaDevServicesProperties}.
 */
class OllamaDevServicesPropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(OllamaDevServicesProperties.CONFIG_PREFIX)
                .isEqualTo("arconia.dev.services.ollama");
    }

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        OllamaDevServicesProperties properties = new OllamaDevServicesProperties();

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).contains("ollama/ollama");
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.ALWAYS);
    }

    @Test
    void shouldUpdateValues() {
        OllamaDevServicesProperties properties = new OllamaDevServicesProperties();

        properties.setEnabled(true);
        properties.setImageName("ollama/ollama:0.6.3");
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setShared(DevServicesProperties.Shared.NEVER);

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).isEqualTo("ollama/ollama:0.6.3");
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.NEVER);
    }

}
