package io.arconia.opentelemetry.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for OpenTelemetry.
 */
@ConfigurationProperties(prefix = OpenTelemetryProperties.CONFIG_PREFIX)
public class OpenTelemetryProperties {

    public static final String CONFIG_PREFIX = "arconia.otel";

    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
