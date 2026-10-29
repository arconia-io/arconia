package io.arconia.observation.openinference.ai.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link OpenInferenceEnvironmentPostProcessor}.
 */
class OpenInferenceEnvironmentPostProcessorTests {

    private final OpenInferenceEnvironmentPostProcessor processor = new OpenInferenceEnvironmentPostProcessor();

    @Test
    void postProcessEnvironmentShouldThrowExceptionWhenEnvironmentIsNull() {
        assertThatThrownBy(() -> processor.postProcessEnvironment(null, new SpringApplication()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("environment cannot be null");
    }

    @Test
    void postProcessEnvironmentShouldAddPropertySourceFirst() {
        var environment = new MockEnvironment();
        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getPropertySources().stream().findFirst())
            .hasValueSatisfying(propertySource -> assertThat(propertySource.getName()).isEqualTo("openinference-environment-variable-specification"));
    }

    @Test
    void postProcessEnvironmentShouldNotMapInvalidProperties() {
        var environment = new MockEnvironment()
            .withProperty("OPENINFERENCE_BASE64_IMAGE_MAX_LENGTH", "not-a-number");

        processor.postProcessEnvironment(environment, new SpringApplication());

        String prefix = OpenInferenceProperties.CONFIG_PREFIX;
        assertThat(environment.getProperty(prefix + ".base64-image-max-length")).isNull();
    }

}
