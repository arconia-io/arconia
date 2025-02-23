package io.arconia.opentelemetry.autoconfigure.sdk.tracing;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import io.arconia.opentelemetry.autoconfigure.sdk.exporter.BatchProcessorConfig;

/**
 * Configuration properties for OpenTelemetry tracing.
 */
@ConfigurationProperties(prefix = OpenTelemetryTracingProperties.CONFIG_PREFIX)
public class OpenTelemetryTracingProperties {

    public static final String CONFIG_PREFIX = "arconia.opentelemetry.traces";

    /**
     * How to sample traces.
     */
    private final Sampling sampling = new Sampling();

    /**
     * Constraints for the data captured by spans.
     */
    private final SpanLimits spanLimits = new SpanLimits();

    /**
     * Configuration for the telemetry processor.
     */
    @NestedConfigurationProperty
    private final BatchProcessorConfig processor = new BatchProcessorConfig();

    public OpenTelemetryTracingProperties() {
        this.processor.setScheduleDelay(Duration.ofSeconds(5));
    }

    public Sampling getSampling() {
        return sampling;
    }

    public SpanLimits getSpanLimits() {
        return spanLimits;
    }

    public BatchProcessorConfig getProcessor() {
        return processor;
    }

    public static class Sampling {

        /**
         * How to sample traces.
         */
        private SamplingStrategy strategy = SamplingStrategy.PARENT_BASED_ALWAYS_ON;

        /**
         * Probability of sampling a trace when using {@link SamplingStrategy#TRACE_ID_RATIO}
         * or {@link SamplingStrategy#PARENT_BASED_TRACE_ID_RATIO}.
         */
        private double probability = 0.1;

        public SamplingStrategy getStrategy() {
            return strategy;
        }

        public void setStrategy(SamplingStrategy strategy) {
            this.strategy = strategy;
        }

        public double getProbability() {
            return probability;
        }

        public void setProbability(double probability) {
            this.probability = probability;
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

}
