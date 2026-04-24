package io.arconia.opentelemetry.autoconfigure.exporter.otlp;

import java.time.Duration;

import io.opentelemetry.sdk.common.export.RetryPolicy;

/**
 * Configuration for retrying failed requests.
 */
public class RetryConfig {

    /**
     * Maximum number of retries.
     */
    private int maxAttempts = 5;

    private final BackoffConfig backoffConfig = new BackoffConfig();

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public BackoffConfig getBackoffConfig() {
        return backoffConfig;
    }

    public static final class BackoffConfig {

        /**
         * Initial backoff time.
         */
        private Duration firstBackoff = Duration.ofSeconds(1);

        /**
         * Maximum backoff time.
         */
        private Duration maxBackoff = Duration.ofSeconds(5);

        /**
         * Backoff multiplier.
         */
        private double multiplier = 1.5;

        public Duration getFirstBackoff() {
            return firstBackoff;
        }

        public void setFirstBackoff(Duration firstBackoff) {
            this.firstBackoff = firstBackoff;
        }

        public Duration getMaxBackoff() {
            return maxBackoff;
        }

        public void setMaxBackoff(Duration maxBackoff) {
            this.maxBackoff = maxBackoff;
        }

        public double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(double multiplier) {
            this.multiplier = multiplier;
        }

    }

    public static RetryPolicy buildRetryPolicy(RetryConfig retryConfig) {
        return RetryPolicy.builder()
                .setMaxAttempts(retryConfig.getMaxAttempts())
                .setInitialBackoff(retryConfig.getBackoffConfig().getFirstBackoff())
                .setMaxBackoff(retryConfig.getBackoffConfig().getMaxBackoff())
                .setBackoffMultiplier(retryConfig.getBackoffConfig().getMultiplier())
                .build();
    }

}
