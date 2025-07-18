package io.arconia.opentelemetry.autoconfigure.metrics.exporter;

import io.arconia.opentelemetry.autoconfigure.exporter.OpenTelemetryExporterAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.metrics.OpenTelemetryMetricsProperties;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.console.ConsoleMetricsExporterConfiguration;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp.OtlpMetricsExporterConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenTelemetryMetricsExporterAutoConfiguration}.
 */
class OpenTelemetryMetricsExporterAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenTelemetryExporterAutoConfiguration.class,
                OpenTelemetryMetricsExporterAutoConfiguration.class))
            .withBean(OpenTelemetryMetricsProperties.class, OpenTelemetryMetricsProperties::new);

    @Test
    void autoConfigurationNotActivatedWhenOpenTelemetryDisabled() {
        contextRunner
            .withPropertyValues("arconia.otel.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(OpenTelemetryMetricsExporterAutoConfiguration.class));
    }

    @Test
    void autoConfigurationNotActivatedWhenMetricsDisabled() {
        contextRunner
            .withPropertyValues("arconia.otel.metrics.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(OpenTelemetryMetricsExporterAutoConfiguration.class));
    }

    @Test
    void configurationPropertiesEnabled() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OpenTelemetryMetricsExporterProperties.class);
            assertThat(context).hasSingleBean(OpenTelemetryMetricsExporterAutoConfiguration.class);
        });
    }

    @Test
    void consoleExporterConfigurationImportedWhenEnabled() {
        contextRunner
            .withPropertyValues("arconia.otel.metrics.exporter.type=console")
            .run(context -> {
                assertThat(context).hasSingleBean(ConsoleMetricsExporterConfiguration.class);
                assertThat(context).doesNotHaveBean(OtlpMetricsExporterConfiguration.class);
            });
    }

    @Test
    void otlpExporterConfigurationImportedWhenDefault() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(ConsoleMetricsExporterConfiguration.class);
            assertThat(context).hasSingleBean(OtlpMetricsExporterConfiguration.class);
        });
    }

    @Test
    void otlpExporterConfigurationImportedWhenEnabled() {
        contextRunner
            .withPropertyValues("arconia.otel.metrics.exporter.type=otlp")
            .run(context -> {
                assertThat(context).doesNotHaveBean(ConsoleMetricsExporterConfiguration.class);
                assertThat(context).hasSingleBean(OtlpMetricsExporterConfiguration.class);
            });
    }

}
