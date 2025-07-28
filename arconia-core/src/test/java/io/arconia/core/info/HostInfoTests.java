package io.arconia.core.info;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
