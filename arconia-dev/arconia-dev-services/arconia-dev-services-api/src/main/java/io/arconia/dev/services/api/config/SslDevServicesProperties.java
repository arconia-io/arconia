package io.arconia.dev.services.api.config;

import io.arconia.core.support.Incubating;

/**
 * Properties of a dev service that supports SSL, implemented next to
 * {@link BaseDevServicesProperties} by the properties of such a dev service.
 * <p>
 * Declare the configuration as a nested property named {@code ssl}:
 * <pre>{@code
 * @NestedConfigurationProperty
 * private final SslConfig ssl = new SslConfig();
 * }</pre>
 */
@Incubating
public interface SslDevServicesProperties {

    /**
     * SSL configuration of the dev service.
     */
    SslConfig getSsl();

}
