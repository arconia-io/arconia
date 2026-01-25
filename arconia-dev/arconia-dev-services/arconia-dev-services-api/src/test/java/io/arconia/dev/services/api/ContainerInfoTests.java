package io.arconia.dev.services.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.api.registration.ContainerInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ContainerInfo}.
 */
class ContainerInfoTests {

    @Test
    void whenIdIsNullThenThrow() {
        assertThatThrownBy(() -> new ContainerInfo(null, "image", List.of(), List.of(), Map.of(), "running"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id cannot be null or empty");
    }

    @Test
    void whenIdIsEmptyThenThrow() {
        assertThatThrownBy(() -> new ContainerInfo("", "image", List.of(), List.of(), Map.of(), "running"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id cannot be null or empty");
    }

    @Test
    void whenImageNameIsNullThenThrow() {
        assertThatThrownBy(() -> new ContainerInfo("id123", null, List.of(), List.of(), Map.of(), "running"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("imageName cannot be null or empty");
    }

    @Test
    void whenImageNameIsEmptyThenThrow() {
        assertThatThrownBy(() -> new ContainerInfo("id123", "", List.of(), List.of(), Map.of(), "running"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("imageName cannot be null or empty");
    }

    @Test
    void whenNamesIsNullThenThrow() {
        assertThatThrownBy(() -> new ContainerInfo("id123", "image", null, List.of(), Map.of(), "running"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("names cannot be null");
    }

    @Test
    void whenExposedPortsIsNullThenThrow() {
        assertThatThrownBy(() -> new ContainerInfo("id123", "image", List.of(), null, Map.of(), "running"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exposedPorts cannot be null");
    }

    @Test
    void whenLabelsIsNullThenThrow() {
        assertThatThrownBy(() -> new ContainerInfo("id123", "image", List.of(), List.of(), null, "running"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("labels cannot be null");
    }

    @Test
    void whenStatusIsNullThenThrow() {
        assertThatThrownBy(() -> new ContainerInfo("id123", "image", List.of(), List.of(), Map.of(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("status cannot be null or empty");
    }

    @Test
    void whenStatusIsEmptyThenThrow() {
        assertThatThrownBy(() -> new ContainerInfo("id123", "image", List.of(), List.of(), Map.of(), ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("status cannot be null or empty");
    }

    @Test
    void whenAllFieldsAreValidThenCreate() {
        var names = List.of("container1", "container2");
        var port = new ContainerInfo.ContainerPort("127.0.0.1", 8080, 8080, "tcp");
        var exposedPorts = List.of(port);
        var labels = Map.of("key1", "value1", "key2", "value2");

        var containerInfo = new ContainerInfo("id123", "image", names, exposedPorts, labels, "running");

        assertThat(containerInfo.id()).isEqualTo("id123");
        assertThat(containerInfo.imageName()).isEqualTo("image");
        assertThat(containerInfo.names()).containsExactly("container1", "container2");
        assertThat(containerInfo.exposedPorts()).containsExactly(port);
        assertThat(containerInfo.labels()).containsEntry("key1", "value1").containsEntry("key2", "value2");
        assertThat(containerInfo.status()).isEqualTo("running");
    }

    @Test
    void whenCreatedThenCollectionsAreImmutable() {
        var names = new ArrayList<>(List.of("container1"));
        var port = new ContainerInfo.ContainerPort("127.0.0.1", 8080, 8080, "tcp");
        var exposedPorts = new ArrayList<>(List.of(port));
        var labels = new HashMap<>(Map.of("key1", "value1"));

        var containerInfo = new ContainerInfo("id123", "image", names, exposedPorts, labels, "running");

        // Modify the original collections
        names.add("container2");
        exposedPorts.add(new ContainerInfo.ContainerPort("127.0.0.1", 9090, 9090, "tcp"));
        labels.put("key2", "value2");

        // Verify that the ContainerInfo collections are unchanged (defensive copies were made)
        assertThat(containerInfo.names()).hasSize(1).containsExactly("container1");
        assertThat(containerInfo.exposedPorts()).hasSize(1).containsExactly(port);
        assertThat(containerInfo.labels()).hasSize(1).containsEntry("key1", "value1");

        // Verify that the returned collections are immutable
        assertThatThrownBy(() -> containerInfo.names().add("container3"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> containerInfo.exposedPorts().add(new ContainerInfo.ContainerPort("127.0.0.1", 9090, 9090, "tcp")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> containerInfo.labels().put("key2", "value2"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void whenContainerPortCreatedWithNullFieldsThenCreate() {
        var containerPort = new ContainerInfo.ContainerPort(null, null, null, null);

        assertThat(containerPort.ip()).isNull();
        assertThat(containerPort.privatePort()).isNull();
        assertThat(containerPort.publicPort()).isNull();
        assertThat(containerPort.type()).isNull();
    }

}
