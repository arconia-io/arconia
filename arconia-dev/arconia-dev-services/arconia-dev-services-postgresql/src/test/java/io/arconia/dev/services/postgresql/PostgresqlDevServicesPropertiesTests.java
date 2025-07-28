package io.arconia.dev.services.postgresql;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.core.config.DevServicesProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PostgresqlDevServicesProperties}.
 */
class PostgresqlDevServicesPropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(PostgresqlDevServicesProperties.CONFIG_PREFIX)
                .isEqualTo("arconia.dev.services.postgresql");
    }

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        PostgresqlDevServicesProperties properties = new PostgresqlDevServicesProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).contains("postgres");
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.NEVER);
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    void shouldUpdateValues() {
        PostgresqlDevServicesProperties properties = new PostgresqlDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("postgres:latest");
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setShared(DevServicesProperties.Shared.ALWAYS);
        properties.setStartupTimeout(Duration.ofMinutes(5));

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("postgres:latest");
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.ALWAYS);
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(5));
    }

}
