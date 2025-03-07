package io.arconia.opentelemetry.autoconfigure.instrumentation.micrometer;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = MicrometerProperties.CONFIG_PREFIX)
public class MicrometerProperties {

    public static final String INSTRUMENTATION_NAME = "micrometer";

    public static final String CONFIG_PREFIX = "arconia.otel.instrumentation." + INSTRUMENTATION_NAME;

    /**
     * The base time unit for Micrometer metrics.
     */
    private TimeUnit baseTimeUnit = TimeUnit.MILLISECONDS;

    /**
     * Whether to generate gauge-based Micrometer histograms.
     */
    private boolean histogramGauges = true;

    public TimeUnit getBaseTimeUnit() {
        return baseTimeUnit;
    }

    public void setBaseTimeUnit(TimeUnit baseTimeUnit) {
        this.baseTimeUnit = baseTimeUnit;
    }

    public boolean isHistogramGauges() {
        return histogramGauges;
    }

    public void setHistogramGauges(boolean histogramGauges) {
        this.histogramGauges = histogramGauges;
    }

}
