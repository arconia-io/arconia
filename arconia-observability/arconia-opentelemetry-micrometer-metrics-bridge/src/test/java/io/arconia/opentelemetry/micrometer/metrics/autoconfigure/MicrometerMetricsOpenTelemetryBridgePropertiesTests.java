package io.arconia.opentelemetry.micrometer.metrics.autoconfigure;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MicrometerMetricsOpenTelemetryBridgeProperties}.
 */
class MicrometerMetricsOpenTelemetryBridgePropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(MicrometerMetricsOpenTelemetryBridgeProperties.CONFIG_PREFIX)
                .isEqualTo("arconia.otel.metrics.micrometer-bridge");
    }

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        MicrometerMetricsOpenTelemetryBridgeProperties properties = new MicrometerMetricsOpenTelemetryBridgeProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getBaseTimeUnit()).isEqualTo(TimeUnit.SECONDS);
        assertThat(properties.isHistogramGauges()).isTrue();
    }

    @Test
    void shouldUpdateValue() {
        MicrometerMetricsOpenTelemetryBridgeProperties properties = new MicrometerMetricsOpenTelemetryBridgeProperties();

        properties.setEnabled(false);
        properties.setBaseTimeUnit(TimeUnit.MILLISECONDS);
        properties.setHistogramGauges(false);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getBaseTimeUnit()).isEqualTo(TimeUnit.MILLISECONDS);
        assertThat(properties.isHistogramGauges()).isFalse();
    }

}
