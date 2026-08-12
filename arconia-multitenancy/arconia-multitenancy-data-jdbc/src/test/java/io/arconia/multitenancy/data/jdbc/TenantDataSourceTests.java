package io.arconia.multitenancy.data.jdbc;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import io.arconia.multitenancy.core.context.TenantContext;
import io.arconia.multitenancy.core.exceptions.TenantNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link TenantDataSource}.
 */
class TenantDataSourceTests {

    private final TestDataSource acme = new TestDataSource("acme");

    private final TestDataSource beans = new TestDataSource("beans");

    private final TestDataSource admin = new TestDataSource("admin");

    @Test
    void whenNullDataSourcesThenThrow() {
        assertThatThrownBy(() -> TenantDataSource.builder().dataSources(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("dataSources cannot be null");
    }

    @Test
    void whenNullDataSourceFactoryThenThrow() {
        assertThatThrownBy(() -> TenantDataSource.builder().dataSourceFactory(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("dataSourceFactory cannot be null");
    }

    @Test
    void whenNullDefaultDataSourceThenThrow() {
        assertThatThrownBy(() -> TenantDataSource.builder().defaultDataSource(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("defaultDataSource cannot be null");
    }

    @Test
    void whenEmptyTenantIdentifierThenThrow() {
        assertThatThrownBy(() -> TenantDataSource.builder().dataSource("", acme))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantIdentifier cannot be null or empty");
    }

    @Test
    void whenTenantRegisteredThenUseItsDataSource() throws Exception {
        var tenantDataSource = TenantDataSource.builder().dataSources(Map.of("acme", acme, "beans", beans)).build();

        assertThat(connectionName(tenantDataSource, "acme")).isEqualTo("acme");
        assertThat(connectionName(tenantDataSource, "beans")).isEqualTo("beans");
    }

    @Test
    void whenTenantNotRegisteredThenUseFactory() throws Exception {
        var tenantDataSource = TenantDataSource.builder()
            .dataSource("acme", acme)
            .dataSourceFactory(TestDataSource::new)
            .build();

        assertThat(connectionName(tenantDataSource, "pixie")).isEqualTo("pixie");
    }

    @Test
    void whenTenantNotRegisteredThenFactoryInvokedOnce() throws Exception {
        var invocations = new AtomicInteger();
        var tenantDataSource = TenantDataSource.builder().dataSourceFactory(tenantIdentifier -> {
            invocations.incrementAndGet();
            return new TestDataSource(tenantIdentifier);
        }).build();

        connectionName(tenantDataSource, "pixie");
        connectionName(tenantDataSource, "pixie");

        assertThat(invocations).hasValue(1);
    }

    @Test
    void whenTenantNotRegisteredAndNoFactoryThenThrow() {
        var tenantDataSource = TenantDataSource.builder().dataSource("acme", acme).build();

        assertThatThrownBy(() -> connectionName(tenantDataSource, "pixie"))
            .isInstanceOf(TenantNotFoundException.class)
            .hasMessageContaining("No data source found for tenant 'pixie'")
            .hasMessageContaining("no data source factory is configured");
    }

    @Test
    void whenFactoryReturnsNullThenThrow() {
        var tenantDataSource = TenantDataSource.builder().dataSourceFactory(tenantIdentifier -> null).build();

        assertThatThrownBy(() -> connectionName(tenantDataSource, "pixie"))
            .isInstanceOf(TenantNotFoundException.class)
            .hasMessageContaining("No data source found for tenant 'pixie'");
    }

    @Test
    void whenTenantNotRegisteredThenNeverFallBackToDefault() {
        var tenantDataSource = TenantDataSource.builder()
            .dataSource("acme", acme)
            .defaultDataSource(admin)
            .build();

        assertThatThrownBy(() -> connectionName(tenantDataSource, "pixie"))
            .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void whenNoTenantThenUseDefaultDataSource() throws Exception {
        var tenantDataSource = TenantDataSource.builder().dataSource("acme", acme).defaultDataSource(admin).build();

        assertThat(tenantDataSource.getConnection().getSchema()).isEqualTo("admin");
    }

    @Test
    void whenNoTenantAndNoDefaultThenThrow() {
        var tenantDataSource = TenantDataSource.builder().dataSource("acme", acme).build();

        assertThatThrownBy(tenantDataSource::getConnection).isInstanceOf(TenantNotFoundException.class)
            .hasMessageContaining("No tenant found in the current context");
    }

    @Test
    void whenDelegateFailsThenPropagateSqlException() {
        var failing = new TestDataSource("acme") {
            @Override
            public Connection getConnection() throws SQLException {
                throw new SQLException("Connection refused");
            }
        };
        var tenantDataSource = TenantDataSource.builder().dataSource("acme", failing).build();

        assertThatThrownBy(() -> connectionName(tenantDataSource, "acme")).isInstanceOf(SQLException.class)
            .hasMessage("Connection refused");
    }

    @Test
    void whenDestroyedThenCloseOnlyCreatedDataSources() throws Exception {
        var created = new ArrayList<TestDataSource>();
        var tenantDataSource = TenantDataSource.builder()
            .dataSource("acme", acme)
            .defaultDataSource(admin)
            .dataSourceFactory(tenantIdentifier -> {
                var dataSource = new TestDataSource(tenantIdentifier);
                created.add(dataSource);
                return dataSource;
            })
            .build();
        connectionName(tenantDataSource, "pixie");

        tenantDataSource.destroy();

        assertThat(created).singleElement().satisfies(dataSource -> assertThat(dataSource.closed).isTrue());
        assertThat(acme.closed).isFalse();
        assertThat(admin.closed).isFalse();
    }

    private static String connectionName(TenantDataSource tenantDataSource, String tenantIdentifier) throws Exception {
        return TenantContext.where(tenantIdentifier).call(() -> tenantDataSource.getConnection().getSchema());
    }

    /**
     * A {@link DataSource} whose connections report the owning tenant as their schema, so
     * that tests can assert which data source a call was routed to.
     */
    private static class TestDataSource implements DataSource, AutoCloseable {

        private final String name;

        private boolean closed;

        TestDataSource(String name) {
            this.name = name;
        }

        @Override
        public Connection getConnection() throws SQLException {
            var connection = mock(Connection.class);
            given(connection.getSchema()).willReturn(name);
            return connection;
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return getConnection();
        }

        @Override
        public void close() {
            this.closed = true;
        }

        @Override
        public @Nullable PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) {}

        @Override
        public void setLoginTimeout(int seconds) {}

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public Logger getParentLogger() {
            return Logger.getGlobal();
        }

        @Override
        public <T> T unwrap(Class<T> iface) {
            return iface.cast(this);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return iface.isInstance(this);
        }

    }

}
