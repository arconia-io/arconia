package io.arconia.dev.service.core.config;

/**
 * Base properties for dev services.
 */
public interface DevServiceProperties {

    boolean isEnabled();

    String getImageName();

    boolean isReusable();

}
