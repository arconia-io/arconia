package io.arconia.opentelemetry.autoconfigure.tracing.exporter.console;

import io.opentelemetry.exporter.logging.LoggingSpanExporter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import io.arconia.opentelemetry.autoconfigure.tracing.ConditionalOnEnabledOpenTelemetryTracing;
import io.arconia.opentelemetry.autoconfigure.tracing.exporter.OpenTelemetryTracingExporterProperties;

/**
 * Auto-configuration for exporting traces to the console.
 */
@AutoConfiguration
@ConditionalOnClass({ LoggingSpanExporter.class })
@ConditionalOnProperty(prefix = OpenTelemetryTracingExporterProperties.CONFIG_PREFIX, name = "type", havingValue = "console")
@ConditionalOnEnabledOpenTelemetryTracing
public class ConsoleTracingExporterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    LoggingSpanExporter consoleSpanExporter() {
        return LoggingSpanExporter.create();
    }

}
