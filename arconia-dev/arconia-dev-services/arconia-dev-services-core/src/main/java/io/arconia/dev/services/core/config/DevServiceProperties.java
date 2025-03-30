package io.arconia.dev.services.core.config;

/**
 * Base properties for dev services.
 */
public interface DevServiceProperties {

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

}
