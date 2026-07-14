package io.arconia.dev.services.core.registration;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.api.registration.ContainerInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link DiscoveredContainer}.
 */
class DiscoveredContainerTests {

    @Test
    void whenContainerInfoIsNullThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DiscoveredContainer(null, "localhost"))
                .withMessageContaining("containerInfo cannot be null");
    }

    @Test
    void whenHostIsEmptyThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DiscoveredContainer(containerInfo(), ""))
                .withMessageContaining("host cannot be null or empty");
    }

    @Test
    void whenContainerPortIsMappedThenReturnHostPort() {
        var container = new DiscoveredContainer(containerInfo(), "localhost");

        assertThat(container.mappedPort(5432)).isEqualTo(54321);
    }

    @Test
    void whenContainerPortIsNotMappedThenThrow() {
        var container = new DiscoveredContainer(containerInfo(), "localhost");

        assertThatIllegalStateException()
                .isThrownBy(() -> container.mappedPort(8080))
                .withMessageContaining("No host port mapped to container port 8080");
    }

    @Test
    void whenContainerPortIsMappedForMultipleIpFamiliesThenIpv4BindingWins() {
        var container = new DiscoveredContainer(containerInfo(List.of(
                new ContainerInfo.ContainerPort("::", 5432, 54322, "tcp"),
                new ContainerInfo.ContainerPort("0.0.0.0", 5432, 54321, "tcp"))), "localhost");

        assertThat(container.mappedPort(5432)).isEqualTo(54321);
    }

    @Test
    void whenContainerPortIsMappedForBothTcpAndUdpThenTcpMappingWins() {
        var container = new DiscoveredContainer(containerInfo(List.of(
                new ContainerInfo.ContainerPort("0.0.0.0", 5432, 54322, "udp"),
                new ContainerInfo.ContainerPort("0.0.0.0", 5432, 54321, "tcp"))), "localhost");

        assertThat(container.mappedPort(5432)).isEqualTo(54321);
    }

    @Test
    void whenContainerPortIsMappedOnlyOverUdpThenThrow() {
        var container = new DiscoveredContainer(containerInfo(List.of(
                new ContainerInfo.ContainerPort("0.0.0.0", 5432, 54321, "udp"))), "localhost");

        assertThatIllegalStateException()
                .isThrownBy(() -> container.mappedPort(5432))
                .withMessageContaining("No host port mapped to container port 5432");
    }

    private static ContainerInfo containerInfo() {
        return containerInfo(List.of(
                new ContainerInfo.ContainerPort("0.0.0.0", 5432, 54321, "tcp"),
                new ContainerInfo.ContainerPort(null, 9090, null, "tcp")));
    }

    private static ContainerInfo containerInfo(List<ContainerInfo.ContainerPort> exposedPorts) {
        return ContainerInfo.builder()
                .id("abc123")
                .imageName("postgres:latest")
                .names(List.of("shared-postgres"))
                .exposedPorts(exposedPorts)
                .labels(Map.of())
                .status("running")
                .build();
    }

}
