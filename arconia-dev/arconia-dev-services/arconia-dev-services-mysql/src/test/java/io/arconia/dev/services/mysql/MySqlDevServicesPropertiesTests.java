package io.arconia.dev.services.mysql;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.core.config.DevServicesProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MySqlDevServicesProperties}.
 */
class MySqlDevServicesPropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(MySqlDevServicesProperties.CONFIG_PREFIX)
                .isEqualTo("arconia.dev.services.mysql");
    }

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        MySqlDevServicesProperties properties = new MySqlDevServicesProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).contains("mysql");
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.NEVER);
    }

    @Test
    void shouldUpdateValues() {
        MySqlDevServicesProperties properties = new MySqlDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("mysql:latest");
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setShared(DevServicesProperties.Shared.ALWAYS);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("mysql:latest");
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.ALWAYS);
    }

}
