package io.arconia.opentelemetry.autoconfigure.metrics.exporter.console;

import io.opentelemetry.exporter.logging.LoggingMetricExporter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.opentelemetry.autoconfigure.metrics.exporter.ConditionalOnOpenTelemetryMetricsExporter;

/**
 * Auto-configuration for exporting metrics to the console.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ LoggingMetricExporter.class })
@ConditionalOnOpenTelemetryMetricsExporter("console")
public final class ConsoleMetricsExporterConfiguration {

    @Bean
    @ConditionalOnMissingBean
    LoggingMetricExporter consoleMetricExporter() {
        return LoggingMetricExporter.create();
    }

}
