package io.arconia.opentelemetry.autoconfigure.sdk.metrics.exporter.console;

import io.opentelemetry.exporter.logging.LoggingMetricExporter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import io.arconia.opentelemetry.autoconfigure.sdk.metrics.ConditionalOnOpenTelemetryMetrics;
import io.arconia.opentelemetry.autoconfigure.sdk.metrics.exporter.OpenTelemetryMetricsExporterProperties;

/**
 * Auto-configuration for exporting metrics to the console.
 */
@AutoConfiguration
@ConditionalOnClass({ LoggingMetricExporter.class })
@ConditionalOnProperty(prefix = OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX, name = "type", havingValue = "console")
@ConditionalOnOpenTelemetryMetrics
public class ConsoleMetricsExporterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    LoggingMetricExporter consoleMetricExporter() {
        return LoggingMetricExporter.create();
    }

}
