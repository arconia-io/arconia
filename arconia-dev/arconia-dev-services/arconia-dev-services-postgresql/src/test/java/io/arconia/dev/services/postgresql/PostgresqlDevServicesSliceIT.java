package io.arconia.dev.services.postgresql;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.postgresql.PostgreSQLContainer;

import io.arconia.dev.services.tests.DisabledIfDockerUnavailable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the PostgreSQL Dev Service in a test slice: the slice picks up the dev
 * service with no further configuration, and connects to its database instead of an embedded one.
 */
@DataJdbcTest
@DisabledIfDockerUnavailable
class PostgresqlDevServicesSliceIT {

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private PostgreSQLContainer container;

    @Test
    void sliceConnectsToTheDevService() {
        String version = jdbcClient.sql("select version()").query(String.class).single();

        assertThat(container.isRunning()).isTrue();
        assertThat(version).startsWith("PostgreSQL");
    }

    /**
     * The application the slice is taken from, as a test would have one in the project.
     */
    @SpringBootApplication(proxyBeanMethods = false)
    static class TestApplication {}

}
