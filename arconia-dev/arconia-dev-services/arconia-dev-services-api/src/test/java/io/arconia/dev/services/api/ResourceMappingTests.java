package io.arconia.dev.services.api;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.api.config.ResourceMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ResourceMapping}.
 */
class ResourceMappingTests {

    @Test
    void whenSourcePathIsEmptyThenThrow() {
        assertThatThrownBy(() -> new ResourceMapping("", "/etc/config/test.txt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sourcePath cannot be null or empty");
    }

    @Test
    void whenContainerPathIsEmptyThenThrow() {
        assertThatThrownBy(() -> new ResourceMapping("test.txt", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("containerPath cannot be null or empty");
    }

    @Test
    void whenPathsAreValidThenCreate() {
        var mapping = new ResourceMapping("classpath:test.txt", "/etc/config/test.txt");

        assertThat(mapping.sourcePath()).isEqualTo("classpath:test.txt");
        assertThat(mapping.containerPath()).isEqualTo("/etc/config/test.txt");
    }

}
