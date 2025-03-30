package io.arconia.dev.services.postgresql;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PostgresqlDevServiceProperties}.
 */
class PostgresqlDevServicePropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(PostgresqlDevServiceProperties.CONFIG_PREFIX)
                .isEqualTo("arconia.dev.services.postgresql");
    }

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        PostgresqlDevServiceProperties properties = new PostgresqlDevServiceProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).isEqualTo("postgres:17.4-alpine");
        assertThat(properties.isReusable()).isFalse();
    }

    @Test
    void shouldUpdateValues() {
        PostgresqlDevServiceProperties properties = new PostgresqlDevServiceProperties();

        properties.setEnabled(false);
        properties.setImageName("postgres:latest");
        properties.setReusable(false);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("postgres:latest");
        assertThat(properties.isReusable()).isFalse();
    }

}
