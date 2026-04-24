package io.arconia.opentelemetry.autoconfigure.exporter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import io.arconia.opentelemetry.autoconfigure.ConditionalOnOpenTelemetry;

/**
 * Auto-configuration for OpenTelemetry exporters.
 */
@AutoConfiguration
@ConditionalOnOpenTelemetry
@EnableConfigurationProperties(OpenTelemetryExporterProperties.class)
public final class OpenTelemetryExporterAutoConfiguration {

}
