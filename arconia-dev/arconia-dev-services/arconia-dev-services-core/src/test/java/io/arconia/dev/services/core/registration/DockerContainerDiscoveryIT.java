package io.arconia.dev.services.core.registration;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.api.registration.DevServiceLabels;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link DockerContainerDiscovery}.
 */
@EnabledIfDockerAvailable
class DockerContainerDiscoveryIT {

    private static final DockerImageName NGINX_IMAGE = DockerImageName.parse("nginx:alpine3.24");

    private final DockerContainerDiscovery discovery = new DockerContainerDiscovery();

    private final String serviceName = "discovery-" + UUID.randomUUID();

    @Test
    void onlyRunningDiscoverableContainersOfTheDevServiceAreDiscovered() {
        try (GenericContainer<?> discoverable = container(serviceName, "true", "another-application");
             GenericContainer<?> notDiscoverable = container(serviceName, "false", "another-application");
             GenericContainer<?> otherService = container(serviceName + "-other", "true", "another-application");
             GenericContainer<?> paused = container(serviceName, "true", "another-application");
             GenericContainer<?> own = container(serviceName, "true", DevServiceLabels.ownerId())
        ) {
            discoverable.start();
            notDiscoverable.start();
            otherService.start();
            paused.start();
            own.start();
            DockerClientFactory.lazyClient().pauseContainerCmd(paused.getContainerId()).exec();

            try {
                assertThat(discovery.discover(serviceName))
                        .singleElement()
                        .satisfies(candidate -> {
                            assertThat(candidate.containerInfo().id()).isEqualTo(discoverable.getContainerId());
                            assertThat(candidate.host()).isEqualTo(discoverable.getHost());
                            assertThat(candidate.mappedPort(80)).isEqualTo(discoverable.getMappedPort(80));
                        });
            } finally {
                DockerClientFactory.lazyClient().unpauseContainerCmd(paused.getContainerId()).exec();
            }
        }
    }

    @Test
    void nothingDiscoveredWhenNoContainerIsDiscoverable() {
        assertThat(discovery.discover(serviceName)).isEmpty();
    }

    private GenericContainer<?> container(String name, String discoverable, String ownerId) {
        return new GenericContainer<>(NGINX_IMAGE)
                .withExposedPorts(80)
                .withLabel(DevServiceLabels.NAME, name)
                .withLabel(DevServiceLabels.DISCOVERABLE, discoverable)
                .withLabel(DevServiceLabels.OWNER, ownerId);
    }

}
