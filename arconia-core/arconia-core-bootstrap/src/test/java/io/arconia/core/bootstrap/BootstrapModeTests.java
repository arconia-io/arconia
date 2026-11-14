package io.arconia.core.bootstrap;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link BootstrapMode}.
 */
class BootstrapModeTests {

    @Test
    void environmentVariableMatchesSystemProperty() {
        assertThat(BootstrapMode.ENVIRONMENT_VARIABLE).isEqualTo(BootstrapMode.SYSTEM_PROPERTY.toUpperCase().replace('.', '_'));
    }

}
