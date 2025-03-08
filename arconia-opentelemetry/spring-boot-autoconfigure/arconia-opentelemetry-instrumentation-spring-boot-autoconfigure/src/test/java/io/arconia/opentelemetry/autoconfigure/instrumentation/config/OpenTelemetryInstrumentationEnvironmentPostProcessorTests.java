package io.arconia.opentelemetry.autoconfigure.instrumentation.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link OpenTelemetryInstrumentationEnvironmentPostProcessor}.
 */
class OpenTelemetryInstrumentationEnvironmentPostProcessorTests {

    private final OpenTelemetryInstrumentationEnvironmentPostProcessor processor = new OpenTelemetryInstrumentationEnvironmentPostProcessor();

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
            .hasValueSatisfying(propertySource -> assertThat(propertySource.getName()).isEqualTo("arconia-opentelemetry-instrumentation"));
    }

    @Test
    void postProcessEnvironmentShouldMapLogbackAppenderProperties() {
        var environment = new MockEnvironment()
            .withProperty("otel.instrumentation.logback-appender.enabled", "true");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("arconia.otel.instrumentation.logback-appender.enabled")).isEqualTo("true");
    }

    @Test
    void postProcessEnvironmentShouldMapMicrometerProperties() {
        var environment = new MockEnvironment()
            .withProperty("otel.instrumentation.micrometer.enabled", "true");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("arconia.otel.instrumentation.micrometer.enabled")).isEqualTo("true");
    }

    @Test
    void postProcessEnvironmentShouldNotProcessPropertiesWhenCompatibilityIsDisabled() {
        var environment = new MockEnvironment()
            .withProperty("arconia.otel.compatibility.opentelemetry", "false")
            .withProperty("otel.instrumentation.micrometer.enabled", "true")
            .withProperty("otel.instrumentation.logback-appender.enabled", "true");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("arconia.otel.instrumentation.micrometer.enabled")).isNull();
        assertThat(environment.getProperty("arconia.otel.instrumentation.logback-appender.enabled")).isNull();
    }

    @Test
    void postProcessEnvironmentShouldProcessPropertiesWhenCompatibilityIsEnabled() {
        var environment = new MockEnvironment()
            .withProperty("arconia.otel.compatibility.opentelemetry", "true")
            .withProperty("otel.instrumentation.micrometer.enabled", "true")
            .withProperty("otel.instrumentation.logback-appender.enabled", "true");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("arconia.otel.instrumentation.micrometer.enabled")).isEqualTo("true");
        assertThat(environment.getProperty("arconia.otel.instrumentation.logback-appender.enabled")).isEqualTo("true");
    }

    @Test
    void getOrderShouldReturnLowestPrecedence() {
        assertThat(processor.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
    }

}
