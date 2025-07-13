package io.arconia.opentelemetry.autoconfigure.instrumentation.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import io.arconia.opentelemetry.autoconfigure.instrumentation.logback.LogbackAppenderProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link OpenTelemetryInstrumentationPropertyAdapters}.
 */
class OpenTelemetryInstrumentationPropertyAdaptersTests {

    @Test
    void logbackAppenderShouldThrowExceptionWhenEnvironmentIsNull() {
        assertThatThrownBy(() -> OpenTelemetryInstrumentationPropertyAdapters.logbackAppender(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("environment cannot be null");
    }

    @Test
    void logbackAppenderShouldMapProperties() {
        var environment = new MockEnvironment()
            .withProperty("otel.instrumentation.logback-appender.enabled", "true")
            .withProperty("otel.instrumentation.logback-appender.experimental-log-attributes", "true")
            .withProperty("otel.instrumentation.logback-appender.experimental.capture-code-attributes", "true")
            .withProperty("otel.instrumentation.logback-appender.experimental.capture-marker-attribute", "true")
            .withProperty("otel.instrumentation.logback-appender.experimental.capture-key-value-pair-attributes", "true")
            .withProperty("otel.instrumentation.logback-appender.experimental.capture-logger-context-attributes", "true")
            .withProperty("otel.instrumentation.logback-appender.experimental.capture-mdc-attributes", "true")
            .withProperty("otel.instrumentation.logback-appender.experimental.capture-arguments", "true")
            .withProperty("otel.instrumentation.logback-appender.experimental.capture-logstash-attributes", "true");

        var adapter = OpenTelemetryInstrumentationPropertyAdapters.logbackAppender(environment);

        assertThat(adapter.getArconiaProperties().get(LogbackAppenderProperties.CONFIG_PREFIX + ".enabled"))
            .isEqualTo(true);
        assertThat(adapter.getArconiaProperties().get(LogbackAppenderProperties.CONFIG_PREFIX + ".capture-experimental-attributes"))
            .isEqualTo(true);
        assertThat(adapter.getArconiaProperties().get(LogbackAppenderProperties.CONFIG_PREFIX + ".capture-code-attributes"))
            .isEqualTo(true);
        assertThat(adapter.getArconiaProperties().get(LogbackAppenderProperties.CONFIG_PREFIX + ".capture-marker-attribute"))
            .isEqualTo(true);
        assertThat(adapter.getArconiaProperties().get(LogbackAppenderProperties.CONFIG_PREFIX + ".capture-key-value-pair-attributes"))
            .isEqualTo(true);
        assertThat(adapter.getArconiaProperties().get(LogbackAppenderProperties.CONFIG_PREFIX + ".capture-logger-context"))
            .isEqualTo(true);
        assertThat(adapter.getArconiaProperties().get(LogbackAppenderProperties.CONFIG_PREFIX + ".capture-mdc-attributes"))
            .isEqualTo(true);
        assertThat(adapter.getArconiaProperties().get(LogbackAppenderProperties.CONFIG_PREFIX + ".capture-arguments"))
            .isEqualTo(true);
        assertThat(adapter.getArconiaProperties().get(LogbackAppenderProperties.CONFIG_PREFIX + ".capture-logstash-attributes"))
            .isEqualTo(true);
    }

}
