package io.arconia.dev.services.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.tests.BaseDevServicesPropertiesTests;

/**
 * Unit tests for {@link KeycloakDevServicesProperties}.
 */
class KeycloakDevServicesPropertiesTests extends BaseDevServicesPropertiesTests<KeycloakDevServicesProperties> {


    @Override
    protected KeycloakDevServicesProperties createProperties() {
        return new KeycloakDevServicesProperties();
    }

    @Override
    protected DefaultValues getExpectedDefaults() {
        return DefaultValues.builder()
                .imageName(ArconiaKeycloakContainer.COMPATIBLE_IMAGE_NAME)
                .shared(true)
                .build();
    }


    @Test
    void shouldCreateInstanceWithServiceSpecificDefaultValues() {
        KeycloakDevServicesProperties properties = createProperties();

        assertThat(properties.getManagementConsolePort()).isEqualTo(0);
        assertThat(properties.getUsername()).isEqualTo(KeycloakDevServicesProperties.DEFAULT_USERNAME);
        assertThat(properties.getPassword()).isEqualTo(KeycloakDevServicesProperties.DEFAULT_PASSWORD);
    }



    @Test
    void shouldCreateInstanceWithDefaultValues() {
        KeycloakDevServicesProperties properties = new KeycloakDevServicesProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).contains("keycloak/keycloak");
        assertThat(properties.getPort()).isEqualTo(0);
        assertThat(properties.getEnvironment()).isEmpty();
        assertTrue(properties.isShared());
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(2));
        assertThat(properties.getUsername()).isEqualTo(KeycloakDevServicesProperties.DEFAULT_USERNAME);
        assertThat(properties.getPassword()).isEqualTo(KeycloakDevServicesProperties.DEFAULT_PASSWORD);
    }
    @Test
    void shouldUpdateServiceSpecificValues() {
        KeycloakDevServicesProperties properties = createProperties();

        properties.setManagementConsolePort(ArconiaKeycloakContainer.WEB_CONSOLE_PORT);
        properties.setUsername("myusername");
        properties.setPassword("mypassword");

        assertThat(properties.getManagementConsolePort()).isEqualTo(ArconiaKeycloakContainer.WEB_CONSOLE_PORT);
        assertThat(properties.getUsername()).isEqualTo("myusername");
        assertThat(properties.getPassword()).isEqualTo("mypassword");
    }
}
