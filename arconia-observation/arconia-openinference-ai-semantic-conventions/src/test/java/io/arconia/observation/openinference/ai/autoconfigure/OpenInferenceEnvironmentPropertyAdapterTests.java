package io.arconia.observation.openinference.ai.autoconfigure;

import java.util.Map;

import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.boot.support.SystemEnvironmentPropertySourceEnvironmentPostProcessor;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link OpenInferenceEnvironmentPropertyAdapter}.
 */
class OpenInferenceEnvironmentPropertyAdapterTests {

    @Test
    void whenNullEnvironmentThenThrow() {
        assertThatThrownBy(() -> new OpenInferenceEnvironmentPropertyAdapter(null, LogFactory.getLog(getClass())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("environment cannot be null");
    }

    @Test
    void whenNullLoggerThenThrow() {
        assertThatThrownBy(() -> new OpenInferenceEnvironmentPropertyAdapter(new MockEnvironment(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("logger cannot be null");
    }

    @ParameterizedTest
    @CsvSource({ "true, true", "TRUE, true", "' false ', false", "False, false" })
    void shouldMapBoolean(String value, boolean expected) {
        var environment = new MockEnvironment().withProperty("OPENINFERENCE_HIDE_INPUTS", value);

        var adapter = adapter(environment).mapBoolean("OPENINFERENCE_HIDE_INPUTS", "arconia.hide-inputs");

        assertThat(adapter.getArconiaProperties()).containsEntry("arconia.hide-inputs", expected);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "   " })
    void shouldSkipEmptyValues(String value) {
        var environment = new MockEnvironment().withProperty("OPENINFERENCE_HIDE_INPUTS", value);

        var adapter = adapter(environment).mapBoolean("OPENINFERENCE_HIDE_INPUTS", "arconia.hide-inputs");

        assertThat(adapter.getArconiaProperties()).isEmpty();
    }

    @Test
    void shouldMapInteger() {
        var environment = new MockEnvironment().withProperty("OPENINFERENCE_BASE64_IMAGE_MAX_LENGTH", "64000");

        var adapter = adapter(environment).mapInteger("OPENINFERENCE_BASE64_IMAGE_MAX_LENGTH", "arconia.max-length");

        assertThat(adapter.getArconiaProperties()).containsEntry("arconia.max-length", 64000);
    }

    @Test
    void shouldReportTheEnvironmentVariableAsTheOrigin() {
        var environment = new StandardEnvironment();
        environment.getPropertySources().replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                        Map.of("OPENINFERENCE_HIDE_INPUTS", "true")));
        // Applied by Spring Boot to every application, to track the origin of environment variables.
        new SystemEnvironmentPropertySourceEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());

        var propertySource = new OpenInferenceEnvironmentPropertyAdapter(environment, LogFactory.getLog(getClass()))
                .mapBoolean("OPENINFERENCE_HIDE_INPUTS", "arconia.hide-inputs")
                .toPropertySource("test");

        assertThat(propertySource.getName()).isEqualTo("test");
        assertThat(propertySource.getProperty("arconia.hide-inputs")).isEqualTo(true);
        assertThat(((OriginLookup<String>) propertySource).getOrigin("arconia.hide-inputs"))
                .hasToString("Adapted from the OpenInference property 'OPENINFERENCE_HIDE_INPUTS' (System Environment Property \"OPENINFERENCE_HIDE_INPUTS\")");
    }

    private static OpenInferenceEnvironmentPropertyAdapter adapter(MockEnvironment environment) {
        return new OpenInferenceEnvironmentPropertyAdapter(environment, LogFactory.getLog(OpenInferenceEnvironmentPropertyAdapterTests.class));
    }

}
