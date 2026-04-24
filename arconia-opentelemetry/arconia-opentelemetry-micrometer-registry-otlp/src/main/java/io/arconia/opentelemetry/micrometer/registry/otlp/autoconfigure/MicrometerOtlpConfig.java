package io.arconia.opentelemetry.micrometer.registry.otlp.autoconfigure;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.config.validate.Validated;
import io.micrometer.registry.otlp.AggregationTemporality;
import io.micrometer.registry.otlp.HistogramFlavor;
import io.micrometer.registry.otlp.OtlpConfig;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

/**
 * Implementation of {@link OtlpConfig} for OpenTelemetry unified support.
 */
class MicrometerOtlpConfig implements OtlpConfig {

    private final boolean enabled;
    private final String url;
    private final Duration step;
    private final AggregationTemporality aggregationTemporality;
    private final HistogramFlavor histogramFlavor;
    private final Map<String, String> headers;
    private final Map<String, String> resourceAttributes;
    private final int maxScale;
    private final int maxBucketCount;
    private final TimeUnit baseTimeUnit;

    private MicrometerOtlpConfig(Builder builder) {
        Assert.hasText(builder.url, "url cannot be null or empty");

        this.enabled = builder.enabled;
        this.url = builder.url;
        this.step = builder.step;
        this.aggregationTemporality = builder.aggregationTemporality;
        this.histogramFlavor = builder.histogramFlavor;
        this.headers = builder.headers;
        this.resourceAttributes = builder.resourceAttributes;
        this.maxScale = builder.maxScale;
        this.maxBucketCount = builder.maxBucketCount;
        this.baseTimeUnit = builder.baseTimeUnit;
    }

    /**
     * Creates a new builder for MicrometerOtlpConfig.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    @Nullable
    public String get(String key) {
        return null;
    }

    @Override
    public String prefix() {
        return MicrometerRegistryOtlpProperties.CONFIG_PREFIX;
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public String url() {
        return url;
    }

    @Override
    public Duration step() {
        return step;
    }

    @Override
    public Map<String, String> resourceAttributes() {
        return resourceAttributes;
    }

    @Override
    public AggregationTemporality aggregationTemporality() {
        return aggregationTemporality;
    }

    @Override
    public Map<String, String> headers() {
        return headers;
    }

    @Override
    public HistogramFlavor histogramFlavor() {
        return histogramFlavor;
    }

    @Override
    public Map<String, HistogramFlavor> histogramFlavorPerMeter() {
        return OtlpConfig.super.histogramFlavorPerMeter();
    }

    @Override
    public int maxScale() {
        return maxScale;
    }

    @Override
    public int maxBucketCount() {
        return maxBucketCount;
    }

    @Override
    public Map<String, Integer> maxBucketsPerMeter() {
        return OtlpConfig.super.maxBucketsPerMeter();
    }

    @Override
    public TimeUnit baseTimeUnit() {
        return baseTimeUnit;
    }

    @Override
    public Validated<?> validate() {
        return Validated.none();
    }

    /**
     * Builder for MicrometerOtlpConfig.
     */
    static class Builder {
        private boolean enabled = true;
        private String url;
        private Duration step = Duration.ofSeconds(60);
        private AggregationTemporality aggregationTemporality = AggregationTemporality.CUMULATIVE;
        private HistogramFlavor histogramFlavor = HistogramFlavor.EXPLICIT_BUCKET_HISTOGRAM;
        private final Map<String, String> headers = new HashMap<>();
        private final Map<String, String> resourceAttributes = new HashMap<>();
        private int maxScale = 20;
        private int maxBucketCount = 160;
        private TimeUnit baseTimeUnit = TimeUnit.MILLISECONDS;

        private Builder() {}

        Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        Builder url(String url) {
            this.url = url;
            return this;
        }

        Builder step(Duration step) {
            Assert.notNull(step, "step cannot be null");
            this.step = step;
            return this;
        }

        Builder aggregationTemporality(AggregationTemporality aggregationTemporality) {
            Assert.notNull(aggregationTemporality, "aggregationTemporality cannot be null");
            this.aggregationTemporality = aggregationTemporality;
            return this;
        }

        Builder histogramFlavor(HistogramFlavor histogramFlavor) {
            Assert.notNull(histogramFlavor, "histogramFlavor cannot be null");
            this.histogramFlavor = histogramFlavor;
            return this;
        }

        Builder addHeaders(Map<String, String> headers) {
            Assert.notNull(headers, "headers cannot be null");
            Assert.noNullElements(headers.keySet().toArray(), "headers cannot contain null keys");
            this.headers.putAll(new HashMap<>(headers));
            return this;
        }

        Builder addResourceAttributes(Map<String, String> resourceAttributes) {
            Assert.notNull(resourceAttributes, "resourceAttributes cannot be null");
            Assert.noNullElements(resourceAttributes.keySet().toArray(), "resourceAttributes cannot contain null keys");
            this.resourceAttributes.putAll(new HashMap<>(resourceAttributes));
            return this;
        }

        Builder maxScale(int maxScale) {
            this.maxScale = maxScale;
            return this;
        }

        Builder maxBucketCount(int maxBucketCount) {
            this.maxBucketCount = maxBucketCount;
            return this;
        }

        Builder baseTimeUnit(TimeUnit baseTimeUnit) {
            Assert.notNull(baseTimeUnit, "baseTimeUnit cannot be null");
            this.baseTimeUnit = baseTimeUnit;
            return this;
        }

        MicrometerOtlpConfig build() {
            return new MicrometerOtlpConfig(this);
        }

    }

}
