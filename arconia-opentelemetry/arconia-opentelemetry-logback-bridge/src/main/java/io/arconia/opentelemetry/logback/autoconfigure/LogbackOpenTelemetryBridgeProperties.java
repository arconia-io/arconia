package io.arconia.opentelemetry.logback.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = LogbackOpenTelemetryBridgeProperties.CONFIG_PREFIX)
public class LogbackOpenTelemetryBridgeProperties {

    public static final String CONFIG_PREFIX = "arconia.otel.logs.logback-bridge";

    /**
     * Whether to enable the Logback OpenTelemetry Bridge.
     */
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
