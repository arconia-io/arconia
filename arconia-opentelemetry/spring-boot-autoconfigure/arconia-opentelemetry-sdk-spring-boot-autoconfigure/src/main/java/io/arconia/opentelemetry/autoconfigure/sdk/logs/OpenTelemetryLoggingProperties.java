package io.arconia.opentelemetry.autoconfigure.sdk.logs;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import io.arconia.opentelemetry.autoconfigure.sdk.exporter.BatchProcessorConfig;

/**
 * Configuration properties for OpenTelemetry logs.
 */
@ConfigurationProperties(prefix = OpenTelemetryLoggingProperties.CONFIG_PREFIX)
public class OpenTelemetryLoggingProperties {

    public static final String CONFIG_PREFIX = "arconia.opentelemetry.logs";

    /**
     * Constraints for the data captured by log records.
     */
    private final LogLimits logLimits = new LogLimits();

    /**
     * Configuration for the log record batch processor.
     */
    @NestedConfigurationProperty
    private final BatchProcessorConfig processor = new BatchProcessorConfig();

    public OpenTelemetryLoggingProperties() {
        this.processor.setScheduleDelay(Duration.ofSeconds(1));
    }

    public LogLimits getLogLimits() {
        return logLimits;
    }

    public BatchProcessorConfig getProcessor() {
        return processor;
    }

    public static class LogLimits {

        /**
         * The maximum length of an attribute value.
         */
        private int maxAttributeValueLength = Integer.MAX_VALUE;

        /**
         * The maximum number of attributes that can be attached to a log record.
         */
        private int maxNumberOfAttributes = 128;

        public int getMaxAttributeValueLength() {
            return maxAttributeValueLength;
        }

        public void setMaxAttributeValueLength(int maxAttributeValueLength) {
            this.maxAttributeValueLength = maxAttributeValueLength;
        }

        public int getMaxNumberOfAttributes() {
            return maxNumberOfAttributes;
        }

        public void setMaxNumberOfAttributes(int maxNumberOfAttributes) {
            this.maxNumberOfAttributes = maxNumberOfAttributes;
        }

    }

}
