package io.arconia.dev.services.postgresql;

import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArconiaPostgreSqlContainer}.
 */
class ArconiaPostgreSqlContainerTests {

    @Test
    void whenExposedPortsAreNotConfigured() {
        var container = new ArconiaPostgreSqlContainer(new PostgresqlDevServicesProperties());
        container.configure();
        assertThat(container.getPortBindings()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void whenExposedPortsAreConfigured() {
        var properties = new PostgresqlDevServicesProperties();
        properties.setPort(1234);

        var container = new ArconiaPostgreSqlContainer(properties);
        container.configure();

        var portBindings = container.getPortBindings();
        assertThat(portBindings).isNotNull();
        assertThat(portBindings)
                .anyMatch(binding -> binding.startsWith(
                        properties.getPort() + ":" + PostgreSQLContainer.POSTGRESQL_PORT));
    }

}
