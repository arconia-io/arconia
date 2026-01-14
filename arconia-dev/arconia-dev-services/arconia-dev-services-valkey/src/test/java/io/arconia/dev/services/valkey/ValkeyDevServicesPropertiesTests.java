package io.arconia.dev.services.valkey;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.core.config.DevServicesProperties;

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
        assertThat(properties.getPort()).isEqualTo(0);
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.NEVER);
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    void shouldUpdateValues() {
        ValkeyDevServicesProperties properties = new ValkeyDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("ghcr.io/valkey-io/valkey:latest");
        properties.setPort(ArconiaValkeyContainer.VALKEY_PORT);
        properties.setEnvironment(Map.of("VALKEY_PASSWORD", "password"));
        properties.setShared(DevServicesProperties.Shared.ALWAYS);
        properties.setStartupTimeout(Duration.ofMinutes(5));

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("ghcr.io/valkey-io/valkey:latest");
        assertThat(properties.getPort()).isEqualTo(ArconiaValkeyContainer.VALKEY_PORT);
        assertThat(properties.getEnvironment()).containsEntry("VALKEY_PASSWORD", "password");
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.ALWAYS);
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(5));
    }

}
