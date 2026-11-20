package io.arconia.opentelemetry.autoconfigure.metrics;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for OpenTelemetry metrics.
 */
@ConfigurationProperties(prefix = OpenTelemetryMetricsProperties.CONFIG_PREFIX)
public class OpenTelemetryMetricsProperties {

    public static final String CONFIG_PREFIX = "arconia.otel.metrics";

    /**
     * Maximum number of distinct points per metric.
     */
    private int cardinalityLimit = 2000;

    public int getCardinalityLimit() {
        return this.cardinalityLimit;
    }

    public void setCardinalityLimit(int cardinalityLimit) {
        this.cardinalityLimit = cardinalityLimit;
    }

}
