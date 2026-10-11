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
        var port = new ContainerInfo.ContainerPort("127.0.0.1", 8080, 8080, "tcp");
        return Stream.of(
                Arguments.of("id", (ThrowingCallable) () -> new ContainerInfo("", "image", List.of(), List.of(port), Map.of(), "running", "Up 5 minutes")),
                Arguments.of("imageName", (ThrowingCallable) () -> new ContainerInfo("id", "", List.of(), List.of(port), Map.of(), "running", "Up 5 minutes")),
                Arguments.of("names", (ThrowingCallable) () -> new ContainerInfo("id", "image", null, List.of(port), Map.of(), "running", "Up 5 minutes")),
                Arguments.of("ports", (ThrowingCallable) () -> new ContainerInfo("id", "image", List.of(), null, Map.of(), "running", "Up 5 minutes")),
                Arguments.of("labels", (ThrowingCallable) () -> new ContainerInfo("id", "image", List.of(), List.of(port), null, "running", "Up 5 minutes")),
                Arguments.of("state", (ThrowingCallable) () -> new ContainerInfo("id", "image", List.of(), List.of(port), Map.of(), "", "Up 5 minutes")),
                Arguments.of("status", (ThrowingCallable) () -> new ContainerInfo("id", "image", List.of(), List.of(port), Map.of(), "running", "")));
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

        assertThat(containerInfo.id()).isEqualTo("id123");
        assertThat(containerInfo.imageName()).isEqualTo("image");
        assertThat(containerInfo.names()).containsExactly("container1", "container2");
        assertThat(containerInfo.ports()).containsExactly(port);
        assertThat(containerInfo.labels()).containsEntry("key1", "value1").containsEntry("key2", "value2");
        assertThat(containerInfo.state()).isEqualTo("running");
        assertThat(containerInfo.status()).isEqualTo("Up 5 minutes");
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
        assertThat(containerInfo.names()).hasSize(1).containsExactly("container1");
        assertThat(containerInfo.ports()).hasSize(1).containsExactly(port);
        assertThat(containerInfo.labels()).hasSize(1).containsEntry("key1", "value1");

        // Verify that the returned collections are immutable
        assertThatThrownBy(() -> containerInfo.names().add("container3"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> containerInfo.ports().add(new ContainerInfo.ContainerPort("127.0.0.1", 9090, 9090, "tcp")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> containerInfo.labels().put("key2", "value2"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

}
