package io.arconia.opentelemetry.autoconfigure.logs;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenTelemetryLoggingProperties}.
 */
class OpenTelemetryLoggingPropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(OpenTelemetryLoggingProperties.CONFIG_PREFIX).isEqualTo("arconia.otel.logs");
    }

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        OpenTelemetryLoggingProperties properties = new OpenTelemetryLoggingProperties();

        assertThat(properties.getLimits()).isNotNull();
        assertThat(properties.getProcessor()).isNotNull();

        OpenTelemetryLoggingProperties.LogLimits logLimits = properties.getLimits();
        assertThat(logLimits.getMaxAttributeValueLength()).isEqualTo(Integer.MAX_VALUE);
        assertThat(logLimits.getMaxNumberOfAttributes()).isEqualTo(128);

        OpenTelemetryLoggingProperties.LogRecordProcessorConfig processor = properties.getProcessor();
        assertThat(processor.getScheduleDelay()).isEqualTo(Duration.ofSeconds(1));
        assertThat(processor.getExportTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(processor.getMaxQueueSize()).isEqualTo(2048);
        assertThat(processor.getMaxExportBatchSize()).isEqualTo(512);
        assertThat(processor.isMetrics()).isFalse();
    }

    @Test
    void shouldUpdateValues() {
        OpenTelemetryLoggingProperties properties = new OpenTelemetryLoggingProperties();

        OpenTelemetryLoggingProperties.LogLimits logLimits = properties.getLimits();
        logLimits.setMaxAttributeValueLength(1000);
        logLimits.setMaxNumberOfAttributes(256);

        assertThat(logLimits.getMaxAttributeValueLength()).isEqualTo(1000);
        assertThat(logLimits.getMaxNumberOfAttributes()).isEqualTo(256);

        OpenTelemetryLoggingProperties.LogRecordProcessorConfig processor = properties.getProcessor();
        processor.setScheduleDelay(Duration.ofSeconds(2));
        processor.setExportTimeout(Duration.ofSeconds(60));
        processor.setMaxQueueSize(4096);
        processor.setMaxExportBatchSize(1024);
        processor.setMetrics(true);

        assertThat(processor.getScheduleDelay()).isEqualTo(Duration.ofSeconds(2));
        assertThat(processor.getExportTimeout()).isEqualTo(Duration.ofSeconds(60));
        assertThat(processor.getMaxQueueSize()).isEqualTo(4096);
        assertThat(processor.getMaxExportBatchSize()).isEqualTo(1024);
        assertThat(processor.isMetrics()).isTrue();
    }

}
