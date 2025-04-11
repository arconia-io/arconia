package io.arconia.opentelemetry.autoconfigure.sdk.metrics;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for OpenTelemetry metrics.
 */
@ConfigurationProperties(prefix = OpenTelemetryMetricsProperties.CONFIG_PREFIX)
public class OpenTelemetryMetricsProperties {

    public static final String CONFIG_PREFIX = "arconia.otel.metrics";

    /**
     * The interval between two consecutive exports of metrics.
     */
    private Duration interval = Duration.ofSeconds(60);

    /**
     * Filter for which measurements can become Exemplars.
     */
    private ExemplarFilter exemplarFilter = ExemplarFilter.TRACE_BASED;

    /**
     * Maximum number of distinct points per metric.
     */
    private Integer cardinalityLimit = 2000;

    public Duration getInterval() {
        return interval;
    }

    public void setInterval(Duration interval) {
        this.interval = interval;
    }

    public ExemplarFilter getExemplarFilter() {
        return exemplarFilter;
    }

    public void setExemplarFilter(ExemplarFilter exemplarFilter) {
        this.exemplarFilter = exemplarFilter;
    }

    public Integer getCardinalityLimit() {
        return cardinalityLimit;
    }

    public void setCardinalityLimit(Integer cardinalityLimit) {
        this.cardinalityLimit = cardinalityLimit;
    }

    /**
     * Filter for which measurements can become Exemplars.
     */
    public enum ExemplarFilter {

        /**
         * A filter which makes all measurements eligible for being an exemplar.
         */
        ALWAYS_ON,

        /**
         * A filter which makes no measurements eligible for being an exemplar.
         */
        ALWAYS_OFF,

        /**
         * A filter that only accepts measurements where there is a span in context that is being sampled.
         */
        TRACE_BASED;

    }

}
