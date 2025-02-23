package io.arconia.opentelemetry.autoconfigure.sdk.exporter;

import java.time.Duration;

/**
 * Configuration properties for the telemetry data batch processor used by OpenTelemetry exporters.
 */
public final class BatchProcessorConfig {

    /**
     * The interval between two consecutive exports.
     */
    private Duration scheduleDelay;

    /**
     * The maximum allowed time to export data.
     */
    private Duration exportTimeout = Duration.ofSeconds(30);

    /**
     * The maximum number of records that can be queued before batching.
     */
    private int maxQueueSize = 2048;

    /**
     * The maximum number of records to export in a single batch.
     */
    private int maxExportBatchSize = 512;

    /**
     * Whether to generate metrics for the batch processor.
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
