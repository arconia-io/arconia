package io.arconia.dev.services.postgresql;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.core.config.DevServicesProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PostgresqlDevServicesProperties}.
 */
class PostgresqlDevServicesPropertiesTests {

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        PostgresqlDevServicesProperties properties = new PostgresqlDevServicesProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).contains("postgres");
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.NEVER);
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(2));
        assertThat(properties.getUsername()).isEqualTo("test");
        assertThat(properties.getPassword()).isEqualTo("test");
        assertThat(properties.getDbName()).isEqualTo("test");
        assertThat(properties.getInitScriptPaths()).isEmpty();
    }

    @Test
    void shouldUpdateValues() {
        PostgresqlDevServicesProperties properties = new PostgresqlDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("postgres:latest");
        properties.setPort(ArconiaPostgreSqlContainer.POSTGRESQL_PORT);
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setShared(DevServicesProperties.Shared.ALWAYS);
        properties.setStartupTimeout(Duration.ofMinutes(5));
        properties.setUsername("mytest");
        properties.setPassword("mytest");
        properties.setDbName("mytest");
        properties.setInitScriptPaths(List.of("init.sql"));

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("postgres:latest");
        assertThat(properties.getPort()).isEqualTo(ArconiaPostgreSqlContainer.POSTGRESQL_PORT);
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.ALWAYS);
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(5));
        assertThat(properties.getUsername()).isEqualTo("mytest");
        assertThat(properties.getPassword()).isEqualTo("mytest");
        assertThat(properties.getDbName()).isEqualTo("mytest");
        assertThat(properties.getInitScriptPaths()).containsExactly("init.sql");
    }

}
