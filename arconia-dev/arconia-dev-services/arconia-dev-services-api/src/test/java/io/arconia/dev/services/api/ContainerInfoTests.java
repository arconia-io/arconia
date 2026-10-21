package io.arconia.dev.services.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.arconia.dev.services.api.registration.ContainerInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ContainerInfo}.
 */
class ContainerInfoTests {

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidContainerInfos")
    void whenAComponentIsMissingThenThrow(String component, ThrowingCallable creation) {
        assertThatThrownBy(creation)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(component + " cannot be null");
    }

    static Stream<Arguments> invalidContainerInfos() {
        return Stream.of(
                Arguments.of("id", (ThrowingCallable) () -> validContainerInfo().id("").build()),
                Arguments.of("imageName", (ThrowingCallable) () -> validContainerInfo().imageName("").build()),
                Arguments.of("names", (ThrowingCallable) () -> validContainerInfo().names(null).build()),
                Arguments.of("ports", (ThrowingCallable) () -> validContainerInfo().ports(null).build()),
                Arguments.of("labels", (ThrowingCallable) () -> validContainerInfo().labels(null).build()),
                Arguments.of("state", (ThrowingCallable) () -> validContainerInfo().state("").build()),
                Arguments.of("status", (ThrowingCallable) () -> validContainerInfo().status("").build()));
    }

    private static ContainerInfo.Builder validContainerInfo() {
        return ContainerInfo.builder()
                .id("id")
                .imageName("image")
                .names(List.of())
                .ports(List.of(new ContainerInfo.ContainerPort("127.0.0.1", 8080, 8080, "tcp")))
                .labels(Map.of())
                .state("running")
                .status("Up 5 minutes");
    }

    @Test
    void whenAllFieldsAreValidThenCreate() {
        var names = List.of("container1", "container2");
        var port = new ContainerInfo.ContainerPort("127.0.0.1", 8080, 8080, "tcp");
        var ports = List.of(port);
        var labels = Map.of("key1", "value1", "key2", "value2");

        var containerInfo = ContainerInfo.builder()
                .id("id123")
                .imageName("image")
                .names(names)
                .ports(ports)
                .labels(labels)
                .state("running")
                .status("Up 5 minutes")
                .build();

        assertThat(containerInfo.getId()).isEqualTo("id123");
        assertThat(containerInfo.getImageName()).isEqualTo("image");
        assertThat(containerInfo.getNames()).containsExactly("container1", "container2");
        assertThat(containerInfo.getPorts()).containsExactly(port);
        assertThat(containerInfo.getLabels()).containsEntry("key1", "value1").containsEntry("key2", "value2");
        assertThat(containerInfo.getState()).isEqualTo("running");
        assertThat(containerInfo.getStatus()).isEqualTo("Up 5 minutes");
    }

    @Test
    void whenCreatedThenCollectionsAreImmutable() {
        var names = new ArrayList<>(List.of("container1"));
        var port = new ContainerInfo.ContainerPort("127.0.0.1", 8080, 8080, "tcp");
        var ports = new ArrayList<>(List.of(port));
        var labels = new HashMap<>(Map.of("key1", "value1"));

        var containerInfo = ContainerInfo.builder()
                .id("id123")
                .imageName("image")
                .names(names)
                .ports(ports)
                .labels(labels)
                .state("running")
                .status("Up 5 minutes")
                .build();

        // Modify the original collections
        names.add("container2");
        ports.add(new ContainerInfo.ContainerPort("127.0.0.1", 9090, 9090, "tcp"));
        labels.put("key2", "value2");

        // Verify that the ContainerInfo collections are unchanged (defensive copies were made)
        assertThat(containerInfo.getNames()).hasSize(1).containsExactly("container1");
        assertThat(containerInfo.getPorts()).hasSize(1).containsExactly(port);
        assertThat(containerInfo.getLabels()).hasSize(1).containsEntry("key1", "value1");

        // Verify that the returned collections are immutable
        assertThatThrownBy(() -> containerInfo.getNames().add("container3"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> containerInfo.getPorts().add(new ContainerInfo.ContainerPort("127.0.0.1", 9090, 9090, "tcp")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> containerInfo.getLabels().put("key2", "value2"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

}
