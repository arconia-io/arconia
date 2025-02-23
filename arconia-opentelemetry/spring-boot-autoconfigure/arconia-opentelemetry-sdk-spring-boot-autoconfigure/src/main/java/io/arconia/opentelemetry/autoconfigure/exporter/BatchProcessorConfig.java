package io.arconia.opentelemetry.autoconfigure.exporter;

import java.time.Duration;

/**
 * Configuration properties for the telemetry data batch processor used by OpenTelemetry exporters.
 */
public final class BatchProcessorConfig {

    /**
     * The interval between two consecutive exports of telemetry records.
     */
    private Duration scheduleDelay;

    /**
     * The maximum waiting time for the export operation to complete.
     */
    private Duration exporterTimeout = Duration.ofSeconds(30);

    /**
     * The maximum number of log records that can be queued for export.
     */
    private int maxQueueSize = 2048;

    /**
     * The maximum number of log records that can be exported in a single batch.
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

    public Duration getExporterTimeout() {
        return exporterTimeout;
    }

    public void setExporterTimeout(Duration exporterTimeout) {
        this.exporterTimeout = exporterTimeout;
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
