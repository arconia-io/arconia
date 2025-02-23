package io.arconia.opentelemetry.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Arconia OpenTelemetry.
 */
@ConfigurationProperties(prefix = OpenTelemetryProperties.CONFIG_PREFIX)
public class OpenTelemetryProperties {

    public static final String CONFIG_PREFIX = "arconia.opentelemetry";

}
