package io.arconia.dev.services.artemis;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.core.config.DevServicesProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArtemisDevServicesProperties}.
 */
class ArtemisDevServicesPropertiesTests {

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        ArtemisDevServicesProperties properties = new ArtemisDevServicesProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).contains("apache/activemq-artemis");
        assertThat(properties.getPort()).isEqualTo(0);
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.DEV_MODE);
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(2));
        assertThat(properties.getUsername()).isEqualTo(ArtemisDevServicesProperties.DEFAULT_USERNAME);
        assertThat(properties.getPassword()).isEqualTo(ArtemisDevServicesProperties.DEFAULT_PASSWORD);
    }

    @Test
    void shouldUpdateValues() {
        ArtemisDevServicesProperties properties = new ArtemisDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("apache/activemq-artemis:latest");
        properties.setPort(ArconiaArtemisContainer.WEB_CONSOLE_PORT);
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setShared(DevServicesProperties.Shared.ALWAYS);
        properties.setStartupTimeout(Duration.ofMinutes(5));
        properties.setUsername("myusername");
        properties.setPassword("mypassword");

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("apache/activemq-artemis:latest");
        assertThat(properties.getPort()).isEqualTo(ArconiaArtemisContainer.WEB_CONSOLE_PORT);
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.ALWAYS);
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(5));
        assertThat(properties.getUsername()).isEqualTo("myusername");
        assertThat(properties.getPassword()).isEqualTo("mypassword");
    }
}
