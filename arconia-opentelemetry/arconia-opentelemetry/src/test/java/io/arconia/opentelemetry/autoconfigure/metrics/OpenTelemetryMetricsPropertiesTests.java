package io.arconia.opentelemetry.autoconfigure.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenTelemetryMetricsProperties}.
 */
class OpenTelemetryMetricsPropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(OpenTelemetryMetricsProperties.CONFIG_PREFIX).isEqualTo("arconia.otel.metrics");
    }

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        OpenTelemetryMetricsProperties properties = new OpenTelemetryMetricsProperties();
        assertThat(properties.getCardinalityLimit()).isEqualTo(2000);
    }

    @Test
    void shouldUpdateValues() {
        OpenTelemetryMetricsProperties properties = new OpenTelemetryMetricsProperties();
        properties.setCardinalityLimit(3000);

        assertThat(properties.getCardinalityLimit()).isEqualTo(3000);
    }

}
