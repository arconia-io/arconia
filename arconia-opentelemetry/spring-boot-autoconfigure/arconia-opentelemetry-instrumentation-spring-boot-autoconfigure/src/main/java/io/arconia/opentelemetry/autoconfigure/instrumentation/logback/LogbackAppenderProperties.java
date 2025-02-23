package io.arconia.opentelemetry.autoconfigure.instrumentation.logback;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = LogbackAppenderProperties.CONFIG_PREFIX)
public class LogbackAppenderProperties {

    public static final String INSTRUMENTATION_NAME = "logback-appender";

    public static final String CONFIG_PREFIX = "arconia.otel.instrumentation." + INSTRUMENTATION_NAME;

}
