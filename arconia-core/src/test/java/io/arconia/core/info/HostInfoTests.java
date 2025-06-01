package io.arconia.core.info;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link HostInfo}.
 */
class HostInfoTests {

    @Test
    void whenConstructorThenReturn() {
        HostInfo hostInfo = new HostInfo();
        assertThat(hostInfo).isNotNull();
    }

}
