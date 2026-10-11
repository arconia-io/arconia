package io.arconia.dev.services.tests;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.GenericContainer;

import io.arconia.dev.services.api.config.BaseDevServicesProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base test class for the container class of a dev service.
 *
 * @param <P> the properties type of the dev service
 * @param <C> the container type of the dev service
 */
public abstract class BaseDevServiceContainerTests<P extends BaseDevServicesProperties, C extends GenericContainer<?>> {

    /**
     * Create a new instance of the properties of the dev service, with the default values.
     */
    protected abstract P createProperties();

    /**
     * Create the container of the dev service from the given properties.
     */
    protected abstract C createContainer(P properties);

    /**
     * Configure every fixed port the dev service supports on the given properties, and
     * return the bindings expected on the container as a result, as host port to container port.
     */
    protected abstract Map<Integer, Integer> configureFixedPorts(P properties);

    @Test
    void whenExposedPortsAreNotConfigured() {
        C container = createContainer(createProperties());
        configure(container);

        assertThat(container.getPortBindings()).isEmpty();
    }

    @Test
    void whenExposedPortsAreConfigured() {
        P properties = createProperties();
        Map<Integer, Integer> expectedBindings = configureFixedPorts(properties);
        C container = createContainer(properties);
        configure(container);

        assertThat(container.getPortBindings()).hasSize(expectedBindings.size());
        expectedBindings.forEach((hostPort, containerPort) ->
                assertThat(container.getPortBindings()).anyMatch(binding -> binding.startsWith(hostPort + ":" + containerPort)));
    }

    /**
     * Run the configuration step of the given container, as Testcontainers does right before
     * starting it, which is when a container declares its fixed ports.
     */
    protected static void configure(GenericContainer<?> container) {
        ReflectionTestUtils.invokeMethod(container, "configure");
    }

}
