package io.arconia.opentelemetry.micrometer.registry.otlp.autoconfigure;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Micrometer Metrics Registry OTLP.
 */
@ConfigurationProperties(prefix = MicrometerRegistryOtlpProperties.CONFIG_PREFIX)
public class MicrometerRegistryOtlpProperties {

    public static final String CONFIG_PREFIX = "arconia.otel.exporter.otlp.micrometer";

    /**
     * Whether to enable the Micrometer Metrics Registry OTLP.
     */
    private boolean enabled = true;

    /**
     * Base time unit for Micrometer metrics.
     */
    private TimeUnit baseTimeUnit = TimeUnit.SECONDS;

    /**
     * Max scale to use for exponential histograms, if configured.
     */
    private int maxScale = 20;

    /**
     * Default maximum number of buckets to be used for exponential histograms, if
     * configured. This has no effect on explicit bucket histograms.
     */
    private int maxBucketCount = 160;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public TimeUnit getBaseTimeUnit() {
        return baseTimeUnit;
    }

    public void setBaseTimeUnit(TimeUnit baseTimeUnit) {
        this.baseTimeUnit = baseTimeUnit;
    }

    public int getMaxScale() {
        return maxScale;
    }

    public void setMaxScale(int maxScale) {
        this.maxScale = maxScale;
    }

    public int getMaxBucketCount() {
        return maxBucketCount;
    }

    public void setMaxBucketCount(int maxBucketCount) {
        this.maxBucketCount = maxBucketCount;
    }

}
