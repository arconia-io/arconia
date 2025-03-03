package io.arconia.opentelemetry.autoconfigure.sdk.logs.exporter.console;

import io.opentelemetry.exporter.logging.SystemOutLogRecordExporter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.opentelemetry.autoconfigure.sdk.logs.exporter.OpenTelemetryLoggingExporterProperties;

/**
 * Configuration for exporting logs to the console.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ SystemOutLogRecordExporter.class })
@ConditionalOnProperty(prefix = OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX, name = "type", havingValue = "console")
public class ConsoleLoggingExporterConfiguration {

    @Bean
    @ConditionalOnMissingBean
    SystemOutLogRecordExporter consoleLogRecordExporter() {
        return SystemOutLogRecordExporter.create();
    }

}
