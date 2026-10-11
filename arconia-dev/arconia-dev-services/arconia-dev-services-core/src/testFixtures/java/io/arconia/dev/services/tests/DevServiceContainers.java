package io.arconia.dev.services.tests;

import org.testcontainers.containers.GenericContainer;

import io.arconia.dev.services.api.config.BaseDevServicesProperties;
import io.arconia.dev.services.api.registration.DevServiceLabels;
import io.arconia.dev.services.core.container.ContainerConfigurer;

/**
 * Containers as another application would start them, for tests of the discovery of dev services.
 */
public final class DevServiceContainers {

    private DevServiceContainers() {}

    /**
     * Configure the given container as another application starting it as a discoverable
     * container of the given dev service would: the common dev service properties, and the
     * discovery labels (name, discoverable, owner).
     */
    public static <C extends GenericContainer<?>> C discoverable(C container, BaseDevServicesProperties properties, String serviceName, String ownerId) {
        ContainerConfigurer.apply(container, properties);
        container.withLabel(DevServiceLabels.NAME, serviceName);
        container.withLabel(DevServiceLabels.DISCOVERABLE, "true");
        container.withLabel(DevServiceLabels.OWNER, ownerId);
        return container;
    }

}
