package io.arconia.observation.opentelemetry.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.arconia.observation.opentelemetry.instrumentation.http.OpenTelemetryHttpOptions;

/**
 * Configuration properties for the OpenTelemetry Semantic Conventions.
 */
@ConfigurationProperties(prefix = OpenTelemetryConventionsProperties.CONFIG_PREFIX)
public class OpenTelemetryConventionsProperties {

    public static final String CONFIG_PREFIX = "arconia.observations.conventions.opentelemetry";

    /**
     * Configuration for HTTP semantic conventions.
     */
    private final Http http = new Http();

    /**
     * Configuration for JVM semantic conventions.
     */
    private final Jvm jvm = new Jvm();

    public Http getHttp() {
        return http;
    }

    public Jvm getJvm() {
        return jvm;
    }

    public static class Http extends OpenTelemetryHttpOptions {

        /**
         * Whether to enable HTTP semantic conventions.
         */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

    }

    public static class Jvm {

        /**
         * Whether to enable JVM semantic conventions.
         */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

    }

}
