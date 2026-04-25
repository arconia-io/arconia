package io.arconia.observation.autoconfigure;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = ObservationProperties.CONFIG_PREFIX)
public class ObservationProperties {

    public static final String CONFIG_PREFIX = "arconia.observations";

    /**
     * Semantic conventions for observations.
     */
    private final Conventions conventions = new Conventions();

    public Conventions getConventions() {
        return conventions;
    }

    public static class Conventions {

        /**
         * Type of semantic conventions to use for observations.
         * When set, only the convention module matching this value activates
         * (e.g., "openinference"). When not set, convention modules auto-activate
         * based on classpath detection.
         */
        @Nullable
        private String type;

        @Nullable
        public String getType() {
            return type;
        }

        public void setType(@Nullable String type) {
            this.type = type;
        }

    }

}
