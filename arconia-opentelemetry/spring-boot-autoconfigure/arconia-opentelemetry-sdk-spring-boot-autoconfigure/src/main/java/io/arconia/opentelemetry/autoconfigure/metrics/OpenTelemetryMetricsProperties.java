package io.arconia.opentelemetry.autoconfigure.metrics;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for OpenTelemetry metrics.
 */
@ConfigurationProperties(prefix = OpenTelemetryMetricsProperties.CONFIG_PREFIX)
public class OpenTelemetryMetricsProperties {

    public static final String CONFIG_PREFIX = "arconia.opentelemetry.metrics";

    /**
     * The interval between two consecutive exports of metrics.
     */
    private Duration interval = Duration.ofSeconds(60);

    public Duration getInterval() {
        return interval;
    }

    public void setInterval(Duration interval) {
        this.interval = interval;
    }

}
