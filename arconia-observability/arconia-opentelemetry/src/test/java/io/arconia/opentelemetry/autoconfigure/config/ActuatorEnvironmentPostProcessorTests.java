package io.arconia.opentelemetry.autoconfigure.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.mock.env.MockEnvironment;

import io.arconia.opentelemetry.autoconfigure.logs.exporter.OpenTelemetryLoggingExporterProperties;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.OpenTelemetryMetricsExporterProperties;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.OpenTelemetryTracingExporterProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ActuatorEnvironmentPostProcessor}.
 */
class ActuatorEnvironmentPostProcessorTests {

    private final ActuatorEnvironmentPostProcessor processor = new ActuatorEnvironmentPostProcessor();

    @Test
    void postProcessEnvironmentShouldThrowExceptionWhenEnvironmentIsNull() {
        assertThatThrownBy(() -> processor.postProcessEnvironment(null, new SpringApplication()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("environment cannot be null");
    }

    @Test
    void postProcessEnvironmentShouldAddPropertySourceFirst() {
        var environment = new MockEnvironment()
            .withProperty("arconia.otel.compatibility.actuator", "true");
        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getPropertySources().stream().findFirst())
            .hasValueSatisfying(propertySource -> assertThat(propertySource.getName()).isEqualTo("arconia-opentelemetry-actuator"));
    }

    @Test
    void postProcessEnvironmentShouldMapAllPropertiesWhenCompatibilityIsEnabled() {
        var environment = new MockEnvironment()
            .withProperty("arconia.otel.compatibility.actuator", "true")
            .withProperty("management.otlp.metrics.export.url", "http://localhost:4318/v1/metrics")
            .withProperty("management.otlp.tracing.endpoint", "http://localhost:4318/v1/traces")
            .withProperty("management.otlp.logging.endpoint", "http://localhost:4318/v1/logs");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.endpoint"))
            .isEqualTo("http://localhost:4318/v1/metrics");
        assertThat(environment.getProperty(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.endpoint"))
            .isEqualTo("http://localhost:4318/v1/traces");
        assertThat(environment.getProperty(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.endpoint"))
            .isEqualTo("http://localhost:4318/v1/logs");
    }

    @Test
    void postProcessEnvironmentShouldNotMapPropertiesWhenCompatibilityIsDisabled() {
        var environment = new MockEnvironment()
            .withProperty("arconia.otel.compatibility.actuator", "false")
            .withProperty("management.otlp.metrics.export.url", "http://localhost:4318/v1/metrics")
            .withProperty("management.otlp.tracing.endpoint", "http://localhost:4318/v1/traces")
            .withProperty("management.otlp.logging.endpoint", "http://localhost:4318/v1/logs");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.endpoint")).isNull();
        assertThat(environment.getProperty(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.endpoint")).isNull();
        assertThat(environment.getProperty(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.endpoint")).isNull();
    }

    @Test
    void getOrderShouldReturnLowestPrecedence() {
        assertThat(processor.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
    }

}
