package io.arconia.opentelemetry.autoconfigure.sdk.traces.exporter;

import io.arconia.opentelemetry.autoconfigure.sdk.exporter.OpenTelemetryExporterAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.sdk.traces.exporter.console.ConsoleTracingExporterConfiguration;
import io.arconia.opentelemetry.autoconfigure.sdk.traces.exporter.otlp.OtlpTracingExporterConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenTelemetryTracingExporterAutoConfiguration}.
 */
class OpenTelemetryTracingExporterAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenTelemetryExporterAutoConfiguration.class,
                OpenTelemetryTracingExporterAutoConfiguration.class));

    @Test
    void autoConfigurationNotActivatedWhenOpenTelemetryDisabled() {
        contextRunner
            .withPropertyValues("arconia.otel.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(OpenTelemetryTracingExporterAutoConfiguration.class));
    }

    @Test
    void autoConfigurationNotActivatedWhenTracingDisabled() {
        contextRunner
            .withPropertyValues("arconia.otel.traces.enabled=false")
            .withPropertyValues("management.tracing.enabled=true")
            .run(context -> assertThat(context).doesNotHaveBean(OpenTelemetryTracingExporterAutoConfiguration.class));
    }

    @Test
    void configurationPropertiesEnabled() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OpenTelemetryTracingExporterProperties.class);
            assertThat(context).hasSingleBean(OpenTelemetryTracingExporterAutoConfiguration.class);
        });
    }

    @Test
    void consoleExporterConfigurationImportedWhenEnabled() {
        contextRunner
            .withPropertyValues("arconia.otel.traces.exporter.type=console")
            .run(context -> {
                assertThat(context).hasSingleBean(ConsoleTracingExporterConfiguration.class);
                assertThat(context).doesNotHaveBean(OtlpTracingExporterConfiguration.class);
            });
    }

    @Test
    void otlpExporterConfigurationImportedWhenDefault() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(ConsoleTracingExporterConfiguration.class);
            assertThat(context).hasSingleBean(OtlpTracingExporterConfiguration.class);
        });
    }

    @Test
    void otlpExporterConfigurationImportedWhenEnabled() {
        contextRunner
            .withPropertyValues("arconia.otel.traces.exporter.type=otlp")
            .run(context -> {
                assertThat(context).doesNotHaveBean(ConsoleTracingExporterConfiguration.class);
                assertThat(context).hasSingleBean(OtlpTracingExporterConfiguration.class);
            });
    }

}
