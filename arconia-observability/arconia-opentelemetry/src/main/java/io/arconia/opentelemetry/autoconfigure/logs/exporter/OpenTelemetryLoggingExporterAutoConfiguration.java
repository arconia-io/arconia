package io.arconia.opentelemetry.autoconfigure.logs.exporter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

import io.arconia.opentelemetry.autoconfigure.logs.ConditionalOnOpenTelemetryLogging;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.console.ConsoleLoggingExporterConfiguration;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.otlp.OtlpLoggingExporterConfiguration;

/**
 * Auto-configuration for exporting OpenTelemetry logs.
 */
@AutoConfiguration(before = org.springframework.boot.actuate.autoconfigure.logging.otlp.OtlpLoggingAutoConfiguration.class)
@ConditionalOnOpenTelemetryLogging
@Import({ ConsoleLoggingExporterConfiguration.class, OtlpLoggingExporterConfiguration.class })
@EnableConfigurationProperties(OpenTelemetryLoggingExporterProperties.class)
public class OpenTelemetryLoggingExporterAutoConfiguration {}
