package io.arconia.dev.ui.server.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevToolsRestart}.
 */
class DevToolsRestartTests {

    @Test
    void restartIsNotAvailableWhenDevToolsHasNotSetUpItsRestartSupport() {
        assertThat(DevToolsRestart.isAvailable(getClass().getClassLoader())).isFalse();
    }

}
