package io.arconia.opentelemetry.autoconfigure.metrics;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for OpenTelemetry metrics.
 */
@ConfigurationProperties(prefix = OpenTelemetryMetricsProperties.CONFIG_PREFIX)
public class OpenTelemetryMetricsProperties {

    public static final String CONFIG_PREFIX = "arconia.otel.metrics";

    /**
     * Configuration for exemplars.
     */
    private final Exemplars exemplars = new Exemplars();

    /**
     * Maximum number of distinct points per metric.
     */
    private Integer cardinalityLimit = 2000;

    public Exemplars getExemplars() {
        return this.exemplars;
    }

    public Integer getCardinalityLimit() {
        return this.cardinalityLimit;
    }

    public void setCardinalityLimit(Integer cardinalityLimit) {
        this.cardinalityLimit = cardinalityLimit;
    }

    /**
     * Configuration properties for exemplars.
     */
    public static class Exemplars {

        /**
         * Whether exemplars should be enabled.
         */
        private boolean enabled = true;

        /**
         * Determines which measurements are eligible to become Exemplars.
         */
        private ExemplarFilter filter = ExemplarFilter.TRACE_BASED;

        public boolean isEnabled() {
            return this.enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public ExemplarFilter getFilter() {
            return this.filter;
        }

        public void setFilter(ExemplarFilter filter) {
            this.filter = filter;
        }

    }

    /**
     * Filter for which measurements are eligible to become Exemplars.
     */
    public enum ExemplarFilter {

        /**
         * Filter which makes all measurements eligible for being an exemplar.
         */
        ALWAYS_ON,

        /**
         * Filter which makes no measurements eligible for being an exemplar.
         */
        ALWAYS_OFF,

        /**
         * Filter that only accepts measurements where there is a span in context that is being sampled.
         */
        TRACE_BASED;

    }

}
