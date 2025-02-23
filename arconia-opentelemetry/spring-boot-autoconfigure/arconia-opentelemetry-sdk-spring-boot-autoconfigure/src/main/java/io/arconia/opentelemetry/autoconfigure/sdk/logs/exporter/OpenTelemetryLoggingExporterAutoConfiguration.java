package io.arconia.opentelemetry.autoconfigure.sdk.logs.exporter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import io.arconia.opentelemetry.autoconfigure.sdk.logs.ConditionalOnEnabledOpenTelemetryLogging;

/**
 * Auto-configuration for exporting OpenTelemetry logs.
 */
@AutoConfiguration
@ConditionalOnEnabledOpenTelemetryLogging
@EnableConfigurationProperties(OpenTelemetryLoggingExporterProperties.class)
public class OpenTelemetryLoggingExporterAutoConfiguration {}
