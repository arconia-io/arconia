package io.arconia.opentelemetry.autoconfigure.metrics.exporter;

import io.opentelemetry.sdk.metrics.internal.view.Base2ExponentialHistogramAggregation;
import io.opentelemetry.sdk.metrics.internal.view.ExplicitBucketHistogramAggregation;

/**
 * The strategy for the aggregation of histograms.
 */
public enum HistogramAggregationStrategy {

    /**
     * Uses a base-2 exponential strategy to compress bucket boundaries
     * and an integer scale parameter to manage the histogram resolution.
     * @see Base2ExponentialHistogramAggregation#getDefault()
     */
    BASE2_EXPONENTIAL_BUCKET_HISTOGRAM,

    /**
     * Uses a pre-defined, fixed bucketing strategy to establish histogram bucket boundaries.
     * @see ExplicitBucketHistogramAggregation#getDefault()
     */
    EXPLICIT_BUCKET_HISTOGRAM;

}
