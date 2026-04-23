package io.arconia.observation.autoconfigure;

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
         * Each convention module activates when this value matches its identifier
         * (e.g., "micrometer", "openinference", "opentelemetry").
         */
        private String type = "micrometer";

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

    }

}
