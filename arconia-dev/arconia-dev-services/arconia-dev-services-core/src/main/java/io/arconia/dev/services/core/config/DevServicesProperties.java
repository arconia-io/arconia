package io.arconia.dev.services.core.config;

import java.util.Map;

import org.springframework.context.ApplicationContext;

import io.arconia.boot.mode.ApplicationMode;

/**
 * Base properties for dev services.
 */
public interface DevServicesProperties {

    /**
     * Whether the dev service is enabled.
     */
    boolean isEnabled();

    /**
     * Full name of the container image used in the dev service. Example: "grafana/otel-lgtm:0.8.6".
     */
    String getImageName();

    /**
     * Environment variables to set in the service.
     */
    Map<String,String> getEnvironment();

    /**
     * When the dev service is shared across applications.
     */
    Shared getShared();

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

        public boolean asBoolean(ApplicationContext applicationContext) {
            return switch(this) {
                case ALWAYS -> true;
                case DEV_MODE -> ApplicationMode.DEVELOPMENT.equals(ApplicationMode.of(applicationContext));
                case NEVER -> false;
            };
        }

    }

}
