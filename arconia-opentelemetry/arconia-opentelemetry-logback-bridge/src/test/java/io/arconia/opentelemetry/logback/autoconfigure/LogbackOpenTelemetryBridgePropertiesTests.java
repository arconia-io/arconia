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
        assertThat(properties.isCaptureArguments()).isFalse();
        assertThat(properties.isCaptureTemplate()).isFalse();
        assertThat(properties.isCaptureCodeAttributes()).isFalse();
        assertThat(properties.isCaptureExperimentalAttributes()).isFalse();
        assertThat(properties.isCaptureMarkerAttribute()).isFalse();
        assertThat(properties.getStructuredAttributes().getIncluded()).isEmpty();
        assertThat(properties.getStructuredAttributes().getExcluded()).isEmpty();
        assertThat(properties.getMdcAttributes().getIncluded()).isEmpty();
        assertThat(properties.getMdcAttributes().getExcluded()).isEmpty();
        assertThat(properties.getLoggerContextAttributes().getIncluded()).isEmpty();
        assertThat(properties.getLoggerContextAttributes().getExcluded()).isEmpty();
        assertThat(properties.getNumLogsCapturedBeforeOtelInstall()).isEqualTo(1000);
    }

    @Test
    void shouldUpdateEnabled() {
        LogbackOpenTelemetryBridgeProperties properties = new LogbackOpenTelemetryBridgeProperties();
        properties.setEnabled(false);
        assertThat(properties.isEnabled()).isFalse();
    }

}
