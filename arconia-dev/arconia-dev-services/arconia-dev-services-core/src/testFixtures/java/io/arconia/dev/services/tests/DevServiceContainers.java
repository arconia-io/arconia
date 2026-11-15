package io.arconia.dev.services.tests;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.awaitility.Awaitility;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.DockerClientFactory;
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
     * Wait until the container runtime lists no running discoverable container of the given
     * dev service. Stopping a container returns before the runtime stops listing it, and
     * discovery picks the oldest candidate, so the container a previous test has just stopped
     * would be discovered instead of the one the current test starts.
     */
    public static void awaitNoDiscoverableContainer(String serviceName) {
        Awaitility.await("no running discoverable container of the '%s' dev service".formatted(serviceName))
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(200))
                .until(() -> DockerClientFactory.lazyClient().listContainersCmd()
                        .withLabelFilter(Map.of(
                                DevServiceLabels.NAME, serviceName,
                                DevServiceLabels.DISCOVERABLE, "true"))
                        .withStatusFilter(List.of("running"))
                        .exec()
                        .isEmpty());
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
