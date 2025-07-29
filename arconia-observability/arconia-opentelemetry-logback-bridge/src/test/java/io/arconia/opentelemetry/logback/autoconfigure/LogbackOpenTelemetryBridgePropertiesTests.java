package io.arconia.opentelemetry.logback.autoconfigure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LogbackOpenTelemetryBridgeProperties}.
 */
class LogbackOpenTelemetryBridgePropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(LogbackOpenTelemetryBridgeProperties.CONFIG_PREFIX)
                .isEqualTo("arconia.otel.logs.logback-bridge");
    }

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        LogbackOpenTelemetryBridgeProperties properties = new LogbackOpenTelemetryBridgeProperties();
        assertThat(properties.isEnabled()).isTrue();
    }

    @Test
    void shouldUpdateEnabled() {
        LogbackOpenTelemetryBridgeProperties properties = new LogbackOpenTelemetryBridgeProperties();
        properties.setEnabled(false);
        assertThat(properties.isEnabled()).isFalse();
    }

}
