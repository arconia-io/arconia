package io.arconia.dev.services.postgresql;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PostgresqlDevServicesProperties}.
 */
class PostgresqlDevServicesPropertiesTests {

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        PostgresqlDevServicesProperties properties = new PostgresqlDevServicesProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).isEmpty();
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.getNetworkAliases()).isEmpty();
        assertThat(properties.getPort()).isEqualTo(0);
        assertThat(properties.isShared()).isFalse();
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofSeconds(30));

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
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setNetworkAliases(List.of("network1", "network2"));
        properties.setPort(PostgreSQLContainer.POSTGRESQL_PORT);
        properties.setShared(true);
        properties.setStartupTimeout(Duration.ofMinutes(1));

        properties.setUsername("mytest");
        properties.setPassword("mytest");
        properties.setDbName("mytest");
        properties.setInitScriptPaths(List.of("init.sql"));

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("postgres:latest");
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.getNetworkAliases()).containsExactly("network1", "network2");
        assertThat(properties.getPort()).isEqualTo(PostgreSQLContainer.POSTGRESQL_PORT);
        assertThat(properties.isShared()).isTrue();
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(1));

        assertThat(properties.getUsername()).isEqualTo("mytest");
        assertThat(properties.getPassword()).isEqualTo("mytest");
        assertThat(properties.getDbName()).isEqualTo("mytest");
        assertThat(properties.getInitScriptPaths()).containsExactly("init.sql");
    }

}
