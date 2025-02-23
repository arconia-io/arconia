package io.arconia.opentelemetry.autoconfigure.sdk.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.mock.env.MockEnvironment;

import io.arconia.opentelemetry.autoconfigure.sdk.OpenTelemetryProperties;
import io.arconia.opentelemetry.autoconfigure.sdk.exporter.ExporterType;
import io.arconia.opentelemetry.autoconfigure.sdk.logs.exporter.OpenTelemetryLoggingExporterProperties;
import io.arconia.opentelemetry.autoconfigure.sdk.metrics.exporter.OpenTelemetryMetricsExporterProperties;
import io.arconia.opentelemetry.autoconfigure.sdk.tracing.exporter.OpenTelemetryTracingExporterProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link OpenTelemetrySdkEnvironmentPostProcessor}.
 */
class OpenTelemetrySdkEnvironmentPostProcessorTests {

    private final OpenTelemetrySdkEnvironmentPostProcessor processor = new OpenTelemetrySdkEnvironmentPostProcessor();

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
            .hasValueSatisfying(propertySource -> assertThat(propertySource.getName()).isEqualTo("arconia-opentelemetry-sdk"));
    }

    @Test
    void postProcessEnvironmentShouldMapAllProperties() {
        var environment = new MockEnvironment()
            .withProperty("otel.sdk.disabled", "true")
            .withProperty("otel.service.name", "test-service")
            .withProperty("otel.logs.exporter", "otlp")
            .withProperty("otel.metrics.exporter", "otlp")
            .withProperty("otel.traces.exporter", "otlp");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty(OpenTelemetryProperties.CONFIG_PREFIX + ".enabled")).isEqualTo("false");
        assertThat(environment.getProperty("spring.application.name")).isEqualTo("test-service");
        assertThat(environment.getProperty(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".type")).isEqualTo(ExporterType.OTLP.name());
        assertThat(environment.getProperty(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".type")).isEqualTo(ExporterType.OTLP.name());
        assertThat(environment.getProperty(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".type")).isEqualTo(ExporterType.OTLP.name());
    }

    @Test
    void getOrderShouldReturnLowestPrecedence() {
        assertThat(processor.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
    }

}
