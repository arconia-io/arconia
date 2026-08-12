package io.arconia.multitenancy.tenantdetails.jdbc;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Integration tests for {@link JdbcTenantDetailsService} backed by a PostgreSQL database.
 */
@EnabledIfDockerAvailable
@Testcontainers
class PostgresqlJdbcTenantDetailsServiceIT extends BaseJdbcTenantDetailsServiceTests {

    @Container
    static final PostgreSQLContainer postgresql = new PostgreSQLContainer("postgres:18.4-alpine");

    private final DataSource dataSource = new DriverManagerDataSource(postgresql.getJdbcUrl(),
            postgresql.getUsername(), postgresql.getPassword());

    @Override
    protected String getPlatform() {
        return "postgresql";
    }

    @Override
    protected DataSource getDataSource() {
        return dataSource;
    }

}
