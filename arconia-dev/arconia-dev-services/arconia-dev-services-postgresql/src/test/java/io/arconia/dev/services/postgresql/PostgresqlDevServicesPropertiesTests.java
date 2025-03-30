package io.arconia.dev.services.postgresql;

import org.junit.jupiter.api.Test;

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
        assertThat(properties.getImageName()).isEqualTo("postgres:17.4-alpine");
        assertThat(properties.isReusable()).isFalse();
    }

    @Test
    void shouldUpdateValues() {
        PostgresqlDevServicesProperties properties = new PostgresqlDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("postgres:latest");
        properties.setReusable(false);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("postgres:latest");
        assertThat(properties.isReusable()).isFalse();
    }

}
