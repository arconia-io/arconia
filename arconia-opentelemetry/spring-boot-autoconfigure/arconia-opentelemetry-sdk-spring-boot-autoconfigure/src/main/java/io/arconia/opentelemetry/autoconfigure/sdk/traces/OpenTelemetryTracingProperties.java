package io.arconia.opentelemetry.autoconfigure.sdk.traces;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for OpenTelemetry tracing.
 */
@ConfigurationProperties(prefix = OpenTelemetryTracingProperties.CONFIG_PREFIX)
public class OpenTelemetryTracingProperties {

    public static final String CONFIG_PREFIX = "arconia.otel.traces";

    /**
     * How to sample traces.
     */
    private final Sampling sampling = new Sampling();

    /**
     * Constraints for the data captured by spans.
     */
    private final SpanLimits spanLimits = new SpanLimits();

    /**
     * Configuration for the batch span processor.
     */
    private final SpanProcessorConfig processor = new SpanProcessorConfig();

    public Sampling getSampling() {
        return sampling;
    }

    public SpanLimits getSpanLimits() {
        return spanLimits;
    }

    public SpanProcessorConfig getProcessor() {
        return processor;
    }

    public static class Sampling {

        /**
         * How to sample traces.
         */
        private SamplingStrategy strategy = SamplingStrategy.PARENT_BASED_ALWAYS_ON;

        public SamplingStrategy getStrategy() {
            return strategy;
        }

        public void setStrategy(SamplingStrategy strategy) {
            this.strategy = strategy;
        }

    }

    public static class SpanLimits {
        /**
         * Maximum number of attributes per span.
         */
        private int maxNumberOfAttributes = 128;

        /**
         * Maximum number of events per span.
         */
        private int maxNumberOfEvents = 128;

        /**
         * Maximum number of links per span.
         */
        private int maxNumberOfLinks = 128;

        /**
         * Maximum number of attributes per event.
         */
        private int maxNumberOfAttributesPerEvent = 128;

        /**
         * Maximum number of attributes per link.
         */
        private int maxNumberOfAttributesPerLink = 128;

        /**
         * Maximum length of each attribute value.
         */
        private int maxAttributeValueLength = Integer.MAX_VALUE;

        public int getMaxNumberOfAttributes() {
            return maxNumberOfAttributes;
        }

        public void setMaxNumberOfAttributes(int maxNumberOfAttributes) {
            this.maxNumberOfAttributes = maxNumberOfAttributes;
        }

        public int getMaxNumberOfEvents() {
            return maxNumberOfEvents;
        }

        public void setMaxNumberOfEvents(int maxNumberOfEvents) {
            this.maxNumberOfEvents = maxNumberOfEvents;
        }

        public int getMaxNumberOfLinks() {
            return maxNumberOfLinks;
        }

        public void setMaxNumberOfLinks(int maxNumberOfLinks) {
            this.maxNumberOfLinks = maxNumberOfLinks;
        }

        public int getMaxNumberOfAttributesPerEvent() {
            return maxNumberOfAttributesPerEvent;
        }

        public void setMaxNumberOfAttributesPerEvent(int maxNumberOfAttributesPerEvent) {
            this.maxNumberOfAttributesPerEvent = maxNumberOfAttributesPerEvent;
        }

        public int getMaxNumberOfAttributesPerLink() {
            return maxNumberOfAttributesPerLink;
        }

        public void setMaxNumberOfAttributesPerLink(int maxNumberOfAttributesPerLink) {
            this.maxNumberOfAttributesPerLink = maxNumberOfAttributesPerLink;
        }

        public int getMaxAttributeValueLength() {
            return maxAttributeValueLength;
        }

        public void setMaxAttributeValueLength(int maxAttributeValueLength) {
            this.maxAttributeValueLength = maxAttributeValueLength;
        }
    }

    public enum SamplingStrategy {
            ALWAYS_ON,
            ALWAYS_OFF,
            TRACE_ID_RATIO,
            PARENT_BASED_ALWAYS_ON,
            PARENT_BASED_ALWAYS_OFF,
            PARENT_BASED_TRACE_ID_RATIO;
    }

    /**
     * Configuration for the batch span processor.
     */
    public static class SpanProcessorConfig {

        /**
         * The interval between two consecutive exports.
         */
        private Duration scheduleDelay = Duration.ofSeconds(5);

        /**
         * The maximum allowed time to export spans.
         */
        private Duration exportTimeout = Duration.ofSeconds(30);

        /**
         * The maximum number of spans that can be queued before batching.
         */
        private int maxQueueSize = 2048;

        /**
         * The maximum number of spans to export in a single batch.
         */
        private int maxExportBatchSize = 512;

        /**
         * Whether to generate metrics for the span processor.
         */
        private boolean metrics = false;

        public Duration getScheduleDelay() {
            return scheduleDelay;
        }

        public void setScheduleDelay(Duration scheduleDelay) {
            this.scheduleDelay = scheduleDelay;
        }

        public Duration getExportTimeout() {
            return exportTimeout;
        }

        public void setExportTimeout(Duration exporterTimeout) {
            this.exportTimeout = exporterTimeout;
        }

        public int getMaxQueueSize() {
            return maxQueueSize;
        }

        public void setMaxQueueSize(int maxQueueSize) {
            this.maxQueueSize = maxQueueSize;
        }

        public int getMaxExportBatchSize() {
            return maxExportBatchSize;
        }

        public void setMaxExportBatchSize(int maxExportBatchSize) {
            this.maxExportBatchSize = maxExportBatchSize;
        }

        public boolean isMetrics() {
            return metrics;
        }

        public void setMetrics(boolean metrics) {
            this.metrics = metrics;
        }

    }

}
