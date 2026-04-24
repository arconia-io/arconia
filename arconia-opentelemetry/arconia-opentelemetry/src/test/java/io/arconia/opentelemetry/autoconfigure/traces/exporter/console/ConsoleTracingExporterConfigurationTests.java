package io.arconia.opentelemetry.autoconfigure.traces.exporter.console;

import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ConsoleTracingExporterConfiguration}.
 */
class ConsoleTracingExporterConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConsoleTracingExporterConfiguration.class));

    @Test
    void consoleExporterBeanCreatedWhenEnabled() {
        contextRunner
            .withPropertyValues("arconia.otel.traces.exporter.type=console")
            .run(context -> {
                assertThat(context).hasSingleBean(LoggingSpanExporter.class);
                assertThat(context).hasSingleBean(ConsoleTracingExporterConfiguration.class);
            });
    }

    @Test
    void consoleExporterBeanNotCreatedWhenOtherExporterEnabled() {
        contextRunner
            .withPropertyValues("arconia.otel.traces.exporter.type=otlp")
            .run(context -> {
                assertThat(context).doesNotHaveBean(LoggingSpanExporter.class);
                assertThat(context).doesNotHaveBean(ConsoleTracingExporterConfiguration.class);
            });
    }

    @Test
    void consoleExporterBeanNotCreatedWhenNoPropertySet() {
        contextRunner
            .run(context -> {
                assertThat(context).doesNotHaveBean(LoggingSpanExporter.class);
                assertThat(context).doesNotHaveBean(ConsoleTracingExporterConfiguration.class);
            });
    }

    @Test
    void existingConsoleExporterBeanRespected() {
        contextRunner
            .withPropertyValues("arconia.otel.traces.exporter.type=console")
            .withBean(LoggingSpanExporter.class, LoggingSpanExporter::create)
            .run(context -> {
                assertThat(context).hasSingleBean(LoggingSpanExporter.class);
                assertThat(context).hasSingleBean(ConsoleTracingExporterConfiguration.class);
            });
    }
}
