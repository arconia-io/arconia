package io.arconia.opentelemetry.autoconfigure.metrics;

import org.junit.jupiter.api.Test;

import static io.arconia.opentelemetry.autoconfigure.metrics.OpenTelemetryMetricsProperties.ExemplarFilter;
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
        assertThat(properties.getExemplars().isEnabled()).isTrue();
        assertThat(properties.getExemplars().getFilter()).isEqualTo(ExemplarFilter.TRACE_BASED);
        assertThat(properties.getCardinalityLimit()).isEqualTo(2000);
    }

    @Test
    void shouldUpdateCardinalityLimit() {
        OpenTelemetryMetricsProperties properties = new OpenTelemetryMetricsProperties();
        properties.setCardinalityLimit(3000);
        assertThat(properties.getCardinalityLimit()).isEqualTo(3000);
    }

    @Test
    void shouldUpdateExemplars() {
        OpenTelemetryMetricsProperties properties = new OpenTelemetryMetricsProperties();
        properties.getExemplars().setEnabled(false);
        properties.getExemplars().setFilter(ExemplarFilter.ALWAYS_ON);
        assertThat(properties.getExemplars().isEnabled()).isFalse();
        assertThat(properties.getExemplars().getFilter()).isEqualTo(ExemplarFilter.ALWAYS_ON);
    }

}
