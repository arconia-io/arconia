package io.arconia.multitenancy.tenantdetails.jdbc;

import org.junit.jupiter.api.AfterEach;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * Unit tests for {@link JdbcTenantDetailsService} backed by an H2 database.
 */
class H2JdbcTenantDetailsServiceTests extends BaseJdbcTenantDetailsServiceTests {

    private final EmbeddedDatabase dataSource = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
        .generateUniqueName(true)
        .build();

    @AfterEach
    void shutdownDatabase() {
        dataSource.shutdown();
    }

    @Override
    protected String getPlatform() {
        return "h2";
    }

    @Override
    protected EmbeddedDatabase getDataSource() {
        return dataSource;
    }

}
