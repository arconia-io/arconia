package io.arconia.dev.services.valkey;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.testcontainers.valkey.ValkeyContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ValkeyDevServicesProperties}.
 */
class ValkeyDevServicesPropertiesTests {

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        ValkeyDevServicesProperties properties = new ValkeyDevServicesProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).containsIgnoringCase("ghcr.io/valkey-io/valkey");
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.getNetworkAliases()).isEmpty();
        assertThat(properties.getPort()).isEqualTo(0);
        assertThat(properties.isShared()).isFalse();
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void shouldUpdateValues() {
        ValkeyDevServicesProperties properties = new ValkeyDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("ghcr.io/valkey-io/valkey:latest");
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setNetworkAliases(List.of("network1", "network2"));
        properties.setPort(ValkeyContainer.VALKEY_PORT);
        properties.setShared(true);
        properties.setStartupTimeout(Duration.ofMinutes(1));

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("ghcr.io/valkey-io/valkey:latest");
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.getNetworkAliases()).containsExactly("network1", "network2");
        assertThat(properties.getPort()).isEqualTo(ArconiaValkeyContainer.VALKEY_PORT);
        assertThat(properties.isShared()).isTrue();
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(1));
    }

}
