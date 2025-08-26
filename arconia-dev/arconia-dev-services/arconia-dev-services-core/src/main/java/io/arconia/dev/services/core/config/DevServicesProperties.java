package io.arconia.dev.services.core.config;

import java.time.Duration;
import java.util.Map;

import io.arconia.boot.bootstrap.BootstrapMode;

/**
 * Base properties for dev services.
 */
public interface DevServicesProperties {

    /**
     * Whether the dev service is enabled.
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * Full name of the container image used in the dev service.
     */
    String getImageName();

    /**
     * Environment variables to set in the service.
     */
    default Map<String,String> getEnvironment() {
        return Map.of();
    }

    /**
     * When the dev service is shared across applications.
     */
    default Shared getShared() {
        return Shared.NEVER;
    };

    /**
     * Maximum waiting time for the service to start.
     */
    default Duration getStartupTimeout() {
        return Duration.ofMinutes(2);
    };

    enum Shared {

        /**
         * The service is always shared across applications.
         */
        ALWAYS,

        /**
         * The service is shared across applications only if the application is running in development mode.
         */
        DEV_MODE,

        /**
         * The service is never shared across applications.
         */
        NEVER;

        /**
         * Returns true if the service should be shared based on the current bootstrap mode.
         */
        public boolean asBoolean() {
            return switch(this) {
                case ALWAYS -> true;
                case DEV_MODE -> BootstrapMode.DEV.equals(BootstrapMode.detect());
                case NEVER -> false;
            };
        }

    }

}
