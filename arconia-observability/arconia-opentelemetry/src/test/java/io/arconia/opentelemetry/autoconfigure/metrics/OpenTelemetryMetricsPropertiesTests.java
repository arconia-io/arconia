package io.arconia.opentelemetry.autoconfigure.metrics;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static io.arconia.opentelemetry.autoconfigure.metrics.OpenTelemetryMetricsProperties.ExemplarFilter;

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

        assertThat(properties.getInterval()).isEqualTo(Duration.ofSeconds(60));
        assertThat(properties.getExemplarFilter()).isEqualTo(ExemplarFilter.TRACE_BASED);
    }

    @Test
    void shouldUpdateIntervalValue() {
        OpenTelemetryMetricsProperties properties = new OpenTelemetryMetricsProperties();

        properties.setInterval(Duration.ofSeconds(30));

        assertThat(properties.getInterval()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void shouldUpdateExemplarFilterValue() {
        OpenTelemetryMetricsProperties properties = new OpenTelemetryMetricsProperties();

        properties.setExemplarFilter(ExemplarFilter.ALWAYS_ON);

        assertThat(properties.getExemplarFilter()).isEqualTo(ExemplarFilter.ALWAYS_ON);
    }

}
