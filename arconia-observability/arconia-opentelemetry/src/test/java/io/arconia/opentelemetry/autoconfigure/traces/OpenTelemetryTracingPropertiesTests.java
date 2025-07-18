package io.arconia.opentelemetry.autoconfigure.traces;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenTelemetryTracingProperties}.
 */
class OpenTelemetryTracingPropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(OpenTelemetryTracingProperties.CONFIG_PREFIX).isEqualTo("arconia.otel.traces");
    }

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        OpenTelemetryTracingProperties properties = new OpenTelemetryTracingProperties();

        assertThat(properties.getSampling()).isNotNull();
        assertThat(properties.getLimits()).isNotNull();
        assertThat(properties.getProcessor()).isNotNull();
    }

    @Test
    void shouldHaveDefaultSamplingValues() {
        OpenTelemetryTracingProperties properties = new OpenTelemetryTracingProperties();
        OpenTelemetryTracingProperties.Sampling sampling = properties.getSampling();

        assertThat(sampling.getStrategy()).isEqualTo(OpenTelemetryTracingProperties.SamplingStrategy.PARENT_BASED_ALWAYS_ON);
    }

    @Test
    void shouldUpdateSamplingValues() {
        OpenTelemetryTracingProperties properties = new OpenTelemetryTracingProperties();
        OpenTelemetryTracingProperties.Sampling sampling = properties.getSampling();

        sampling.setStrategy(OpenTelemetryTracingProperties.SamplingStrategy.TRACE_ID_RATIO);

        assertThat(sampling.getStrategy()).isEqualTo(OpenTelemetryTracingProperties.SamplingStrategy.TRACE_ID_RATIO);
    }

    @Test
    void shouldHaveDefaultSpanLimitsValues() {
        OpenTelemetryTracingProperties properties = new OpenTelemetryTracingProperties();
        OpenTelemetryTracingProperties.SpanLimits spanLimits = properties.getLimits();

        assertThat(spanLimits.getMaxNumberOfAttributes()).isEqualTo(128);
        assertThat(spanLimits.getMaxNumberOfEvents()).isEqualTo(128);
        assertThat(spanLimits.getMaxNumberOfLinks()).isEqualTo(128);
        assertThat(spanLimits.getMaxNumberOfAttributesPerEvent()).isEqualTo(128);
        assertThat(spanLimits.getMaxNumberOfAttributesPerLink()).isEqualTo(128);
        assertThat(spanLimits.getMaxAttributeValueLength()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void shouldUpdateSpanLimitsValues() {
        OpenTelemetryTracingProperties properties = new OpenTelemetryTracingProperties();
        OpenTelemetryTracingProperties.SpanLimits spanLimits = properties.getLimits();

        spanLimits.setMaxNumberOfAttributes(256);
        spanLimits.setMaxNumberOfEvents(256);
        spanLimits.setMaxNumberOfLinks(256);
        spanLimits.setMaxNumberOfAttributesPerEvent(256);
        spanLimits.setMaxNumberOfAttributesPerLink(256);
        spanLimits.setMaxAttributeValueLength(1000);

        assertThat(spanLimits.getMaxNumberOfAttributes()).isEqualTo(256);
        assertThat(spanLimits.getMaxNumberOfEvents()).isEqualTo(256);
        assertThat(spanLimits.getMaxNumberOfLinks()).isEqualTo(256);
        assertThat(spanLimits.getMaxNumberOfAttributesPerEvent()).isEqualTo(256);
        assertThat(spanLimits.getMaxNumberOfAttributesPerLink()).isEqualTo(256);
        assertThat(spanLimits.getMaxAttributeValueLength()).isEqualTo(1000);
    }

    @Test
    void shouldHaveDefaultProcessorValues() {
        OpenTelemetryTracingProperties properties = new OpenTelemetryTracingProperties();
        OpenTelemetryTracingProperties.SpanProcessorConfig processor = properties.getProcessor();

        assertThat(processor.getScheduleDelay()).isEqualTo(Duration.ofSeconds(5));
        assertThat(processor.getExportTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(processor.getMaxQueueSize()).isEqualTo(2048);
        assertThat(processor.getMaxExportBatchSize()).isEqualTo(512);
        assertThat(processor.isMetrics()).isFalse();
    }

    @Test
    void shouldUpdateProcessorValues() {
        OpenTelemetryTracingProperties properties = new OpenTelemetryTracingProperties();
        OpenTelemetryTracingProperties.SpanProcessorConfig processor = properties.getProcessor();

        processor.setScheduleDelay(Duration.ofSeconds(10));
        processor.setExportTimeout(Duration.ofSeconds(60));
        processor.setMaxQueueSize(4096);
        processor.setMaxExportBatchSize(1024);
        processor.setMetrics(true);

        assertThat(processor.getScheduleDelay()).isEqualTo(Duration.ofSeconds(10));
        assertThat(processor.getExportTimeout()).isEqualTo(Duration.ofSeconds(60));
        assertThat(processor.getMaxQueueSize()).isEqualTo(4096);
        assertThat(processor.getMaxExportBatchSize()).isEqualTo(1024);
        assertThat(processor.isMetrics()).isTrue();
    }

}
