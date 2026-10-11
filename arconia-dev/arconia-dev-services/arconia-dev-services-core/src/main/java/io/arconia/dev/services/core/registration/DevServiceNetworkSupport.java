package io.arconia.dev.services.core.registration;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

/**
 * Attaches dev service containers to the {@link Network} shared by the dev service containers
 * of the application, when enabled.
 */
final class DevServiceNetworkSupport {

    /**
     * The global property enabling the network shared by the dev service containers of the application.
     */
    static final String NETWORK_ENABLED_PROPERTY = "arconia.dev.services.network.enabled";

    private final Environment environment;

    DevServiceNetworkSupport(Environment environment) {
        Assert.notNull(environment, "environment cannot be null");
        this.environment = environment;
    }

    /**
     * Whether dev service containers should join the shared network (global
     * {@code arconia.dev.services.network.enabled}, default {@code false}).
     */
    boolean isEnabled() {
        return Binder.get(environment).bind(NETWORK_ENABLED_PROPERTY, Boolean.class).orElse(false);
    }

    /**
     * Attach the given container to the shared network, when enabled.
     * <p>
     * The service name is added as a network alias, so every container is reachable by a stable,
     * predictable name next to any {@code network-aliases} configured for it. A network already
     * set on the container (e.g. by a customizer) is honored, and so is a user-defined
     * {@link Network} bean; otherwise, the containers join the network Testcontainers
     * provides for the application.
     * <p>
     * Port mapping is left untouched: the application keeps reaching the container
     * over the host and mapped ports.
     */
    void join(GenericContainer<?> container, String serviceName, BeanFactory beanFactory) {
        Assert.notNull(container, "container cannot be null");
        Assert.hasText(serviceName, "serviceName cannot be null or empty");
        Assert.notNull(beanFactory, "beanFactory cannot be null");

        if (!isEnabled()) {
            return;
        }

        Network network = beanFactory.getBeanProvider(Network.class).getIfAvailable(() -> Network.SHARED);
        if (container.getNetwork() == null) {
            container.withNetwork(network);
        }
        container.withNetworkAliases(serviceName);
    }

}
