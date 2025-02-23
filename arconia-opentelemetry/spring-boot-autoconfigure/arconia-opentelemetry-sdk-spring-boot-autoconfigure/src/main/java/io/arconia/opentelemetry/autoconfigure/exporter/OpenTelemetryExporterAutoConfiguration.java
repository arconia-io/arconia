package io.arconia.opentelemetry.autoconfigure.exporter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import io.arconia.opentelemetry.autoconfigure.ConditionalOnEnabledOpenTelemetry;

/**
 * Auto-configuration for OpenTelemetry exporters.
 */
@AutoConfiguration
@ConditionalOnEnabledOpenTelemetry
@EnableConfigurationProperties(OpenTelemetryExporterProperties.class)
public class OpenTelemetryExporterAutoConfiguration {

}
