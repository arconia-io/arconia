package io.arconia.opentelemetry.autoconfigure.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.mock.env.MockEnvironment;

import io.arconia.opentelemetry.autoconfigure.OpenTelemetryProperties;
import io.arconia.opentelemetry.autoconfigure.exporter.ExporterType;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.OpenTelemetryLoggingExporterProperties;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.OpenTelemetryMetricsExporterProperties;
import io.arconia.opentelemetry.autoconfigure.resource.OpenTelemetryResourceProperties;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.OpenTelemetryTracingExporterProperties;

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
    void postProcessEnvironmentShouldMapAllPropertiesWhenCompatibilityIsEnabled() {
        var environment = new MockEnvironment()
            .withProperty("arconia.otel.compatibility.opentelemetry", "true")
            .withProperty("otel.sdk.disabled", "true")
            .withProperty("otel.service.name", "test-service")
            .withProperty("otel.logs.exporter", "otlp")
            .withProperty("otel.metrics.exporter", "otlp")
            .withProperty("otel.traces.exporter", "otlp");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty(OpenTelemetryProperties.CONFIG_PREFIX + ".enabled")).isEqualTo("false");
        assertThat(environment.getProperty(OpenTelemetryResourceProperties.CONFIG_PREFIX + ".service-name")).isEqualTo("test-service");
        assertThat(environment.getProperty(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".type")).isEqualTo(ExporterType.OTLP.name());
        assertThat(environment.getProperty(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".type")).isEqualTo(ExporterType.OTLP.name());
        assertThat(environment.getProperty(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".type")).isEqualTo(ExporterType.OTLP.name());
    }

    @Test
    void postProcessEnvironmentShouldMapAllPropertiesWhenCompatibilityIsDefault() {
        var environment = new MockEnvironment()
            .withProperty("otel.sdk.disabled", "true")
            .withProperty("otel.service.name", "test-service")
            .withProperty("otel.logs.exporter", "otlp")
            .withProperty("otel.metrics.exporter", "otlp")
            .withProperty("otel.traces.exporter", "otlp");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty(OpenTelemetryProperties.CONFIG_PREFIX + ".enabled")).isEqualTo("false");
        assertThat(environment.getProperty(OpenTelemetryResourceProperties.CONFIG_PREFIX + ".service-name")).isEqualTo("test-service");
        assertThat(environment.getProperty(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".type")).isEqualTo(ExporterType.OTLP.name());
        assertThat(environment.getProperty(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".type")).isEqualTo(ExporterType.OTLP.name());
        assertThat(environment.getProperty(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".type")).isEqualTo(ExporterType.OTLP.name());
    }

    @Test
    void postProcessEnvironmentShouldNotMapPropertiesWhenCompatibilityIsDisabled() {
        var environment = new MockEnvironment()
            .withProperty("arconia.otel.compatibility.opentelemetry", "false")
            .withProperty("otel.sdk.disabled", "true")
            .withProperty("otel.service.name", "test-service")
            .withProperty("otel.logs.exporter", "otlp")
            .withProperty("otel.metrics.exporter", "otlp")
            .withProperty("otel.traces.exporter", "otlp");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty(OpenTelemetryProperties.CONFIG_PREFIX + ".enabled")).isNull();
        assertThat(environment.getProperty(OpenTelemetryResourceProperties.CONFIG_PREFIX + ".service-name")).isNull();
        assertThat(environment.getProperty(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".type")).isNull();
        assertThat(environment.getProperty(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".type")).isNull();
        assertThat(environment.getProperty(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".type")).isNull();
    }

    @Test
    void getOrderShouldReturnLowestPrecedence() {
        assertThat(processor.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
    }

}
