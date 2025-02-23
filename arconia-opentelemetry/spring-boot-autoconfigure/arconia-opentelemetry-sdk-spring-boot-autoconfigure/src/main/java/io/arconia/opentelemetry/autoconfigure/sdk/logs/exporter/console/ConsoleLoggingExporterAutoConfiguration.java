package io.arconia.opentelemetry.autoconfigure.sdk.logs.exporter.console;

import io.opentelemetry.exporter.logging.SystemOutLogRecordExporter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import io.arconia.opentelemetry.autoconfigure.sdk.logs.ConditionalOnEnabledOpenTelemetryLogging;
import io.arconia.opentelemetry.autoconfigure.sdk.logs.exporter.OpenTelemetryLoggingExporterProperties;

/**
 * Auto-configuration for exporting logs to the console.
 */
@AutoConfiguration
@ConditionalOnClass({ SystemOutLogRecordExporter.class })
@ConditionalOnProperty(prefix = OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX, name = "type", havingValue = "console")
@ConditionalOnEnabledOpenTelemetryLogging
public class ConsoleLoggingExporterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    SystemOutLogRecordExporter consoleLogRecordExporter() {
        return SystemOutLogRecordExporter.create();
    }

}
