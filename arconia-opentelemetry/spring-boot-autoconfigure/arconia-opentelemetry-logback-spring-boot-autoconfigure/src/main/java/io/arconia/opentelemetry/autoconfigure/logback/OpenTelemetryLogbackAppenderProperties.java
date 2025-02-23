package io.arconia.opentelemetry.autoconfigure.logback;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = OpenTelemetryLogbackAppenderProperties.CONFIG_PREFIX)
public class OpenTelemetryLogbackAppenderProperties {

    public static final String CONFIG_PREFIX = "arconia.otel.instrumentation.logback-appender";

}
