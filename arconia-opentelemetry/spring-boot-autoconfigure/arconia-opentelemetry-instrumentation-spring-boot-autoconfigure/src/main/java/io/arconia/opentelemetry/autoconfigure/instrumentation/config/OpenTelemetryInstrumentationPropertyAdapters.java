package io.arconia.opentelemetry.autoconfigure.instrumentation.config;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.Assert;

import io.arconia.core.config.adapter.PropertyAdapter;
import io.arconia.opentelemetry.autoconfigure.instrumentation.logback.LogbackAppenderProperties;
import io.arconia.opentelemetry.autoconfigure.instrumentation.micrometer.MicrometerProperties;

/**
 * Provides adapters for OpenTelemetry instrumentation properties.
 */
class OpenTelemetryInstrumentationPropertyAdapters {

    static PropertyAdapter logbackAppender(ConfigurableEnvironment environment) {
        Assert.notNull(environment, "environment cannot be null");

        return PropertyAdapter.builder(environment)
            .mapBoolean("otel.instrumentation.logback-appender.enabled",
                LogbackAppenderProperties.CONFIG_PREFIX + ".enabled")
            .mapBoolean("otel.instrumentation.logback-appender.experimental-log-attributes",
                LogbackAppenderProperties.CONFIG_PREFIX + ".capture-experimental-attributes")
            .mapBoolean("otel.instrumentation.logback-appender.experimental.capture-code-attributes",
                LogbackAppenderProperties.CONFIG_PREFIX + ".capture-code-attributes")
            .mapBoolean("otel.instrumentation.logback-appender.experimental.capture-marker-attribute",
                LogbackAppenderProperties.CONFIG_PREFIX + ".capture-marker-attribute")
            .mapBoolean("otel.instrumentation.logback-appender.experimental.capture-key-value-pair-attributes",
                LogbackAppenderProperties.CONFIG_PREFIX + ".capture-key-value-pair-attributes")
            .mapBoolean("otel.instrumentation.logback-appender.experimental.capture-logger-context-attributes",
                LogbackAppenderProperties.CONFIG_PREFIX + ".capture-logger-context")
            .mapBoolean("otel.instrumentation.logback-appender.experimental.capture-mdc-attributes",
                LogbackAppenderProperties.CONFIG_PREFIX + ".capture-mdc-attributes")
            .mapBoolean("otel.instrumentation.logback-appender.experimental.capture-arguments",
                LogbackAppenderProperties.CONFIG_PREFIX + ".capture-arguments")
            .mapBoolean("otel.instrumentation.logback-appender.experimental.capture-logstash-attributes",
                LogbackAppenderProperties.CONFIG_PREFIX + ".capture-logstash-attributes")
            .build();
    }

    static PropertyAdapter micrometer(ConfigurableEnvironment environment) {
        Assert.notNull(environment, "environment cannot be null");
        
        return PropertyAdapter.builder(environment)
            .mapBoolean("otel.instrumentation.micrometer.enabled",
                MicrometerProperties.CONFIG_PREFIX + ".enabled")
            .build();
    }

}
