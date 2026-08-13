package io.arconia.multitenancy.details.jdbc.actuate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for {@link JdbcTenantDetailsHealthIndicator} backed by an H2
 * database.
 */
class JdbcTenantDetailsHealthIndicatorIT {

    private static final String SCHEMA_SCRIPT = "io/arconia/multitenancy/details/jdbc/autoconfigure/schema-h2.sql";

    private final EmbeddedDatabase dataSource = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
        .generateUniqueName(true)
        .addScript(SCHEMA_SCRIPT)
        .build();

    private final EmbeddedDatabase emptyDataSource = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
        .generateUniqueName(true)
        .build();

    @AfterEach
    void shutdownDatabases() {
        dataSource.shutdown();
        emptyDataSource.shutdown();
    }

    @Test
    void whenNullJdbcClientThenThrow() {
        assertThatThrownBy(() -> new JdbcTenantDetailsHealthIndicator(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("jdbcClient cannot be null");
    }

    @Test
    void whenTableIsReachableThenUp() {
        var health = new JdbcTenantDetailsHealthIndicator(JdbcClient.create(dataSource)).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("tenants", 0L);
    }

    @Test
    void whenTenantsExistThenCountIsReported() {
        JdbcClient.create(dataSource).sql("insert into tenant_details (identifier) values ('acme')").update();

        var health = new JdbcTenantDetailsHealthIndicator(JdbcClient.create(dataSource)).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("tenants", 1L);
    }

    @Test
    void whenTableIsMissingThenDown() {
        var health = new JdbcTenantDetailsHealthIndicator(JdbcClient.create(emptyDataSource)).health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }

}
