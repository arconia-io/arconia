package io.arconia.opentelemetry.autoconfigure.sdk.exporter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import io.arconia.opentelemetry.autoconfigure.sdk.ConditionalOnOpenTelemetry;

/**
 * Auto-configuration for OpenTelemetry exporters.
 */
@AutoConfiguration
@ConditionalOnOpenTelemetry
@EnableConfigurationProperties(OpenTelemetryExporterProperties.class)
public class OpenTelemetryExporterAutoConfiguration {

}
