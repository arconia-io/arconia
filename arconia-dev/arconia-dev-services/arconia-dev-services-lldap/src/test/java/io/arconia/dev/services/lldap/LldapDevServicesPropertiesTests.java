package io.arconia.dev.services.lldap;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.core.config.DevServicesProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LldapDevServicesProperties}.
 */
class LldapDevServicesPropertiesTests {

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        LldapDevServicesProperties properties = new LldapDevServicesProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).contains("lldap/lldap");
        assertThat(properties.getPort()).isEqualTo(0);
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.NEVER);
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    void shouldUpdateValues() {
        LldapDevServicesProperties properties = new LldapDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("lldap/lldap:latest");
        properties.setPort(ArconiaLldapContainer.LLDAP_WEB_CONSOLE_PORT);
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setShared(DevServicesProperties.Shared.ALWAYS);
        properties.setStartupTimeout(Duration.ofMinutes(5));

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("lldap/lldap:latest");
        assertThat(properties.getPort()).isEqualTo(ArconiaLldapContainer.LLDAP_WEB_CONSOLE_PORT);
            assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.ALWAYS);
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(5));

    }

}
