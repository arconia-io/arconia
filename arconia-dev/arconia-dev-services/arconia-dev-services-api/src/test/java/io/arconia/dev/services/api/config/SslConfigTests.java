package io.arconia.dev.services.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ssl.SslBundle;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SslConfig}.
 */
class SslConfigTests {

    @Test
    void protocolDefaultsToTheSpringBootOne() {
        SslConfig config = new SslConfig();

        assertThat(config.getPem().getProtocol()).isEqualTo(SslBundle.DEFAULT_PROTOCOL);
        assertThat(config.getJks().getProtocol()).isEqualTo(SslBundle.DEFAULT_PROTOCOL);
    }

}
