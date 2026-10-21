package io.arconia.dev.services.core.ssl;

import org.springframework.boot.ssl.SslBundle;
import org.testcontainers.containers.Container;

import io.arconia.core.support.Incubating;

/**
 * Configures the container of a dev service to use SSL with the given SSL bundle, before the
 * container is started. The bundle is built from the {@code ssl} properties of the dev service.
 * Use {@link SslBundleFiles} to obtain the certificates and keys in the formats the service expects.
 *
 * @param <C> the type of the container the dev service runs in
 */
@FunctionalInterface
@Incubating
public interface DevServiceSslConfigurer<C extends Container<?>> {

    /**
     * Configure the given container to use SSL with the given bundle.
     */
    void configure(C container, SslBundle bundle);

}
