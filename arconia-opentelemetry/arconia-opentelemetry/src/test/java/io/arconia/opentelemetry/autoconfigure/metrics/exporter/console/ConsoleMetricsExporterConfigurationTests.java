package io.arconia.opentelemetry.autoconfigure.metrics.exporter.console;

import io.opentelemetry.exporter.logging.LoggingMetricExporter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ConsoleMetricsExporterConfiguration}.
 */
class ConsoleMetricsExporterConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConsoleMetricsExporterConfiguration.class));

    @Test
    void consoleExporterBeanCreatedWhenEnabled() {
        contextRunner
            .withPropertyValues("arconia.otel.metrics.exporter.type=console")
            .run(context -> {
                assertThat(context).hasSingleBean(LoggingMetricExporter.class);
                assertThat(context).hasSingleBean(ConsoleMetricsExporterConfiguration.class);
            });
    }

    @Test
    void consoleExporterBeanNotCreatedWhenOtherExporterEnabled() {
        contextRunner
            .withPropertyValues("arconia.otel.metrics.exporter.type=otlp")
            .run(context -> {
                assertThat(context).doesNotHaveBean(LoggingMetricExporter.class);
                assertThat(context).doesNotHaveBean(ConsoleMetricsExporterConfiguration.class);
            });
    }

    @Test
    void consoleExporterBeanNotCreatedWhenNoPropertySet() {
        contextRunner
            .run(context -> {
                assertThat(context).doesNotHaveBean(LoggingMetricExporter.class);
                assertThat(context).doesNotHaveBean(ConsoleMetricsExporterConfiguration.class);
            });
    }

    @Test
    void existingConsoleExporterBeanRespected() {
        contextRunner
            .withPropertyValues("arconia.otel.metrics.exporter.type=console")
            .withBean(LoggingMetricExporter.class, LoggingMetricExporter::create)
            .run(context -> {
                assertThat(context).hasSingleBean(LoggingMetricExporter.class);
                assertThat(context).hasSingleBean(ConsoleMetricsExporterConfiguration.class);
            });
    }
}
