package io.arconia.opentelemetry.autoconfigure.traces.exporter.console;

import io.opentelemetry.exporter.logging.LoggingSpanExporter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.opentelemetry.autoconfigure.traces.exporter.ConditionalOnOpenTelemetryTracingExporter;

/**
 * Auto-configuration for exporting traces to the console.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ LoggingSpanExporter.class })
@ConditionalOnOpenTelemetryTracingExporter("console")
public final class ConsoleTracingExporterConfiguration {

    @Bean
    @ConditionalOnMissingBean
    LoggingSpanExporter consoleSpanExporter() {
        return LoggingSpanExporter.create();
    }

}
