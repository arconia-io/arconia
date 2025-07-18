package io.arconia.opentelemetry.micrometer.metrics.autoconfigure;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MicrometerMetricsOpenTelemetryProperties}.
 */
class MicrometerMetricsOpenTelemetryPropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(MicrometerMetricsOpenTelemetryProperties.CONFIG_PREFIX)
                .isEqualTo("arconia.otel.metrics.micrometer-bridge.opentelemetry-api");
    }

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        MicrometerMetricsOpenTelemetryProperties properties = new MicrometerMetricsOpenTelemetryProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getBaseTimeUnit()).isEqualTo(TimeUnit.SECONDS);
        assertThat(properties.isHistogramGauges()).isTrue();
    }

    @Test
    void shouldUpdateEnabled() {
        MicrometerMetricsOpenTelemetryProperties properties = new MicrometerMetricsOpenTelemetryProperties();

        properties.setEnabled(false);

        assertThat(properties.isEnabled()).isFalse();
    }

    @Test
    void shouldUpdateBaseTimeUnit() {
        MicrometerMetricsOpenTelemetryProperties properties = new MicrometerMetricsOpenTelemetryProperties();

        properties.setBaseTimeUnit(TimeUnit.MILLISECONDS);

        assertThat(properties.getBaseTimeUnit()).isEqualTo(TimeUnit.MILLISECONDS);
    }

    @Test
    void shouldUpdateHistogramGauges() {
        MicrometerMetricsOpenTelemetryProperties properties = new MicrometerMetricsOpenTelemetryProperties();

        properties.setHistogramGauges(false);

        assertThat(properties.isHistogramGauges()).isFalse();
    }

}
