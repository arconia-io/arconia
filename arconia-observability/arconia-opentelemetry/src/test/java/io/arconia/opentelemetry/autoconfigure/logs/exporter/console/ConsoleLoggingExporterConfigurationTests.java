package io.arconia.opentelemetry.autoconfigure.logs.exporter.console;

import io.opentelemetry.exporter.logging.SystemOutLogRecordExporter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ConsoleLoggingExporterConfiguration}.
 */
class ConsoleLoggingExporterConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConsoleLoggingExporterConfiguration.class));

    @Test
    void consoleExporterBeanCreatedWhenEnabled() {
        contextRunner
            .withPropertyValues("arconia.otel.logs.exporter.type=console")
            .run(context -> {
                assertThat(context).hasSingleBean(SystemOutLogRecordExporter.class);
                assertThat(context).hasSingleBean(ConsoleLoggingExporterConfiguration.class);
            });
    }

    @Test
    void consoleExporterBeanNotCreatedWhenOtherExporterEnabled() {
        contextRunner
            .withPropertyValues("arconia.otel.logs.exporter.type=otlp")
            .run(context -> {
                assertThat(context).doesNotHaveBean(SystemOutLogRecordExporter.class);
                assertThat(context).doesNotHaveBean(ConsoleLoggingExporterConfiguration.class);
            });
    }

    @Test
    void consoleExporterBeanNotCreatedWhenNoPropertySet() {
        contextRunner
            .run(context -> {
                assertThat(context).doesNotHaveBean(SystemOutLogRecordExporter.class);
                assertThat(context).doesNotHaveBean(ConsoleLoggingExporterConfiguration.class);
            });
    }

    @Test
    void existingConsoleExporterBeanRespected() {
        contextRunner
            .withPropertyValues("arconia.otel.logs.exporter.type=console")
            .withBean(SystemOutLogRecordExporter.class, SystemOutLogRecordExporter::create)
            .run(context -> {
                assertThat(context).hasSingleBean(SystemOutLogRecordExporter.class);
                assertThat(context).hasSingleBean(ConsoleLoggingExporterConfiguration.class);
            });
    }
}
