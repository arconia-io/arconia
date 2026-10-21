package io.arconia.dev.services.tests;

import java.util.Map;

import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.Transferable;

import io.arconia.dev.services.api.config.DevServicesProperties;
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
    public static <C extends GenericContainer<?>> C discoverable(C container, DevServicesProperties properties, String serviceName, String ownerId) {
        ContainerConfigurer.apply(container, properties);
        container.withLabel(DevServiceLabels.NAME, serviceName);
        container.withLabel(DevServiceLabels.DISCOVERABLE, "true");
        container.withLabel(DevServiceLabels.OWNER, ownerId);
        return container;
    }

    /**
     * The files the given container copies into itself at startup, by target path. Testcontainers
     * keeps the copies declared as {@link Transferable} in a field without a public accessor.
     */
    @SuppressWarnings("unchecked")
    public static Map<Transferable, String> copiedFiles(GenericContainer<?> container) {
        Map<Transferable, String> files = (Map<Transferable, String>) ReflectionTestUtils.getField(container, "copyToTransferableContainerPathMap");
        return (files != null) ? files : Map.of();
    }

}
