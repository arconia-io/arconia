package io.arconia.dev.services.ollama;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.tests.BaseDevServicesPropertiesTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OllamaDevServicesProperties}.
 */
class OllamaDevServicesPropertiesTests extends BaseDevServicesPropertiesTests<OllamaDevServicesProperties> {

    @Override
    protected OllamaDevServicesProperties createProperties() {
        return new OllamaDevServicesProperties();
    }

    @Override
    protected DefaultValues getExpectedDefaults() {
        return DefaultValues.builder()
                .imageName(ArconiaOllamaContainer.COMPATIBLE_IMAGE_NAME)
                .shared(true)
                .startupTimeout(Duration.ofMinutes(2))
                .build();
    }

    @Test
    void shouldCreateInstanceWithServiceSpecificDefaultValues() {
        OllamaDevServicesProperties properties = createProperties();
        assertThat(properties.isIgnoreNativeService()).isFalse();
        assertThat(properties.isOpenaiCompatibility()).isTrue();
        assertThat(properties.getModels()).isEmpty();
    }

    @Test
    void shouldUpdateServiceSpecificValues() {
        OllamaDevServicesProperties properties = createProperties();
        properties.setIgnoreNativeService(true);
        assertThat(properties.isIgnoreNativeService()).isTrue();
        properties.setOpenaiCompatibility(false);
        assertThat(properties.isOpenaiCompatibility()).isFalse();
        properties.setModels(List.of("qwen3:0.6b", "all-minilm"));
        assertThat(properties.getModels()).containsExactly("qwen3:0.6b", "all-minilm");
    }

}
