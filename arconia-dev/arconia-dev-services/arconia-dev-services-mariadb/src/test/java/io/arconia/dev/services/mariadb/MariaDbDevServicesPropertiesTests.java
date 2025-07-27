package io.arconia.dev.services.mariadb;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.core.config.DevServicesProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MariaDbDevServicesProperties}.
 */
class MariaDbDevServicesPropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(MariaDbDevServicesProperties.CONFIG_PREFIX)
                .isEqualTo("arconia.dev.services.mariadb");
    }

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        MariaDbDevServicesProperties properties = new MariaDbDevServicesProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).contains("mariadb");
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.NEVER);
    }

    @Test
    void shouldUpdateValues() {
        MariaDbDevServicesProperties properties = new MariaDbDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("mariadb:latest");
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setShared(DevServicesProperties.Shared.ALWAYS);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("mariadb:latest");
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.ALWAYS);
    }

}
