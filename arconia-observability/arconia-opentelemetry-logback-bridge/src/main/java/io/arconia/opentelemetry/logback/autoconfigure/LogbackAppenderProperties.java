package io.arconia.opentelemetry.logback.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = LogbackAppenderProperties.CONFIG_PREFIX)
public class LogbackAppenderProperties {

    public static final String CONFIG_PREFIX = "arconia.otel.logs.logback-bridge";

    /**
     * Whether to enable the Logback Appender OpenTelemetry bridge.
     */
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
