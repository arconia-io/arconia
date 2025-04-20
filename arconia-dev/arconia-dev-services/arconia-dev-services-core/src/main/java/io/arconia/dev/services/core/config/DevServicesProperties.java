package io.arconia.dev.services.core.config;

import java.util.Map;

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
     * Whether the container used in the dev service is reusable across applications.
     */
    boolean isReusable();

    /**
     * Environment variables to set in the container.
     */
    Map<String,String> getEnvironment();

}
