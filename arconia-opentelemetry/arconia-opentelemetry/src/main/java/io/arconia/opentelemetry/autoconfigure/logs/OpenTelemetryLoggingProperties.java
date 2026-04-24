package io.arconia.opentelemetry.autoconfigure.logs;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for OpenTelemetry logs.
 */
@ConfigurationProperties(prefix = OpenTelemetryLoggingProperties.CONFIG_PREFIX)
public class OpenTelemetryLoggingProperties {

    public static final String CONFIG_PREFIX = "arconia.otel.logs";

    /**
     * Constraints for the data captured by log records.
     */
    private final LogLimits limits = new LogLimits();

    /**
     * Configuration for the batch log record processor.
     */
    private final LogRecordProcessorConfig processor = new LogRecordProcessorConfig();

    public LogLimits getLimits() {
        return limits;
    }

    public LogRecordProcessorConfig getProcessor() {
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

    /**
     * Configuration for the batch log record processor.
     */
    public static class LogRecordProcessorConfig {

        /**
         * The interval between two consecutive exports.
         */
        private Duration scheduleDelay = Duration.ofSeconds(1);

        /**
         * The maximum allowed time to export log records.
         */
        private Duration exportTimeout = Duration.ofSeconds(30);

        /**
         * The maximum number of log records that can be queued before batching.
         */
        private int maxQueueSize = 2048;

        /**
         * The maximum number of log records to export in a single batch.
         */
        private int maxExportBatchSize = 512;

        /**
         * Whether to generate metrics for the log record processor.
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
