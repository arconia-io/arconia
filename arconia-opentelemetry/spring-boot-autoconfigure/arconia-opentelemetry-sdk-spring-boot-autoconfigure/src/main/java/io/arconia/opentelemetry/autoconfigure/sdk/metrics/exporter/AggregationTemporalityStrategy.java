package io.arconia.opentelemetry.autoconfigure.sdk.metrics.exporter;

import io.opentelemetry.sdk.metrics.export.AggregationTemporalitySelector;

/**
 * The temporality of the aggregation of metrics.
 */
public enum AggregationTemporalityStrategy {

    /**
     * All instruments will have cumulative temporality.
     * @see AggregationTemporalitySelector#alwaysCumulative()
     */
    CUMULATIVE,

    /**
     * Counter (sync and async) and histograms will be delta,
     * up-down counters (sync and async) will be cumulative.
     * @see AggregationTemporalitySelector#deltaPreferred()
     */
    DELTA,

    /**
     * Sync counter and histograms will be delta,
     * async counter and up-down counters (sync and async) will be cumulative.
     * @see AggregationTemporalitySelector#lowMemory()
     */
    LOW_MEMORY;

}
