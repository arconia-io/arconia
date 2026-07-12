package io.arconia.dev.services.core.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link ContainerUtils}.
 */
class ContainerUtilsTests {

    @Test
    void isFixedPortWhenPortIsZeroThenReturnFalse() {
        assertThat(ContainerUtils.isFixedPort(0)).isFalse();
    }

    @Test
    void isFixedPortWhenPortIsValidThenReturnTrue() {
        assertThat(ContainerUtils.isFixedPort(1234)).isTrue();
        assertThat(ContainerUtils.isFixedPort(65535)).isTrue();
    }

    @Test
    void isFixedPortWhenPortIsOutOfRangeThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ContainerUtils.isFixedPort(-1))
                .withMessageContaining("port must be between 0 and 65535");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ContainerUtils.isFixedPort(65536))
                .withMessageContaining("port must be between 0 and 65535");
    }

}
