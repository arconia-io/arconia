package io.arconia.dev.services.api;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.api.config.VolumeMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link VolumeMapping}.
 */
class VolumeMappingTests {

    @Test
    void whenHostPathIsEmptyThenThrow() {
        assertThatThrownBy(() -> new VolumeMapping("", "/container/path"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hostPath cannot be null or empty");
    }

    @Test
    void whenContainerPathIsEmptyThenThrow() {
        assertThatThrownBy(() -> new VolumeMapping("/host/path", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("containerPath cannot be null or empty");
    }

    @Test
    void whenPathsAreValidThenCreate() {
        var mapping = new VolumeMapping("/host/path", "/container/path");

        assertThat(mapping.hostPath()).isEqualTo("/host/path");
        assertThat(mapping.containerPath()).isEqualTo("/container/path");
    }

}
