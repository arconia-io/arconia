package io.arconia.observation.opentelemetry.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Auto-configuration for OpenTelemetry Semantic Conventions.
 */
@AutoConfiguration
@EnableConfigurationProperties(OpenTelemetryConventionsProperties.class)
public final class OpenTelemetryConventionsAutoConfiguration {

}
