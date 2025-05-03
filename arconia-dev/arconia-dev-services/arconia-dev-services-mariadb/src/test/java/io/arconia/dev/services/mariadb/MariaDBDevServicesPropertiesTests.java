package io.arconia.dev.services.mariadb;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MariaDBDevServicesProperties}.
 */
class MariaDBDevServicesPropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(MariaDBDevServicesProperties.CONFIG_PREFIX)
                .isEqualTo("arconia.dev.services.mariadb");
    }

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        MariaDBDevServicesProperties properties = new MariaDBDevServicesProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).isEqualTo("mariadb:11.7.2");
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.isReusable()).isFalse();
    }

    @Test
    void shouldUpdateValues() {
        MariaDBDevServicesProperties properties = new MariaDBDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("mariadb:latest");
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setReusable(false);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("mariadb:latest");
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.isReusable()).isFalse();
    }

}
