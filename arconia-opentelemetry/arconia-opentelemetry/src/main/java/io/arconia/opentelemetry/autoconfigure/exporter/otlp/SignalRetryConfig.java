package io.arconia.opentelemetry.autoconfigure.exporter.otlp;

import java.time.Duration;

import org.jspecify.annotations.Nullable;

/**
 * Configuration for retrying failed requests for a single signal. Each setting that isn't set
 * falls back to the one in {@link RetryConfig}, shared by all signals.
 */
public class SignalRetryConfig {

    /**
     * Maximum number of attempts, including the original request. Must be between 2 and 5.
     */
    @Nullable
    private Integer maxAttempts;

    private final BackoffConfig backoffConfig = new BackoffConfig();

    @Nullable
    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(@Nullable Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public BackoffConfig getBackoffConfig() {
        return backoffConfig;
    }

    public static final class BackoffConfig {

        /**
         * Initial backoff time.
         */
        @Nullable
        private Duration firstBackoff;

        /**
         * Maximum backoff time.
         */
        @Nullable
        private Duration maxBackoff;

        /**
         * Backoff multiplier.
         */
        @Nullable
        private Double multiplier;

        @Nullable
        public Duration getFirstBackoff() {
            return firstBackoff;
        }

        public void setFirstBackoff(@Nullable Duration firstBackoff) {
            this.firstBackoff = firstBackoff;
        }

        @Nullable
        public Duration getMaxBackoff() {
            return maxBackoff;
        }

        public void setMaxBackoff(@Nullable Duration maxBackoff) {
            this.maxBackoff = maxBackoff;
        }

        @Nullable
        public Double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(@Nullable Double multiplier) {
            this.multiplier = multiplier;
        }

    }

}
