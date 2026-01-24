package io.arconia.dev.services.keycloak;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.core.config.DevServicesProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link KeycloakDevServicesProperties}.
 */
class KeycloakDevServicesPropertiesTests {

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        KeycloakDevServicesProperties properties = new KeycloakDevServicesProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).contains("keycloak/keycloak");
        assertThat(properties.getPort()).isEqualTo(0);
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.DEV_MODE);
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(2));
        assertThat(properties.getUsername()).isEqualTo(KeycloakDevServicesProperties.DEFAULT_USERNAME);
        assertThat(properties.getPassword()).isEqualTo(KeycloakDevServicesProperties.DEFAULT_PASSWORD);
    }

    @Test
    void shouldUpdateValues() {
        KeycloakDevServicesProperties properties = new KeycloakDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("keycloak/keycloak:latest");
        properties.setPort(8080);
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setShared(DevServicesProperties.Shared.ALWAYS);
        properties.setStartupTimeout(Duration.ofMinutes(5));
        properties.setUsername("myusername");
        properties.setPassword("mypassword");

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("keycloak/keycloak:latest");
        assertThat(properties.getPort()).isEqualTo(8080);
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.ALWAYS);
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(5));
        assertThat(properties.getUsername()).isEqualTo("myusername");
        assertThat(properties.getPassword()).isEqualTo("mypassword");
    }
}
