package io.arconia.opentelemetry.autoconfigure.sdk.traces.exporter.console;

import io.arconia.opentelemetry.autoconfigure.sdk.traces.ConditionalOnOpenTelemetryTracing;
import io.arconia.opentelemetry.autoconfigure.sdk.traces.exporter.OpenTelemetryTracingExporterProperties;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for exporting traces to the console.
 */
@AutoConfiguration
@ConditionalOnClass({ LoggingSpanExporter.class })
@ConditionalOnProperty(prefix = OpenTelemetryTracingExporterProperties.CONFIG_PREFIX, name = "type", havingValue = "console")
@ConditionalOnOpenTelemetryTracing
public class ConsoleTracingExporterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    LoggingSpanExporter consoleSpanExporter() {
        return LoggingSpanExporter.create();
    }

}
