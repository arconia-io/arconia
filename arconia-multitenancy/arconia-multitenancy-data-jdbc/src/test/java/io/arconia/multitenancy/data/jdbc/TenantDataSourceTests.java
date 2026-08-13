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
import io.arconia.multitenancy.core.exceptions.TenantVerificationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
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

    @Test
    void whenTenantIdentifierIsInvalidThenFactoryIsNotInvoked() {
        var invocations = new AtomicInteger();
        var tenantDataSource = TenantDataSource.builder()
            .dataSourceFactory(identifier -> {
                invocations.incrementAndGet();
                return new TestDataSource(identifier);
            })
            .build();

        TenantContext.where("acme?socketFactory=evil").run(() -> {
            assertThatThrownBy(tenantDataSource::getConnection)
                .isInstanceOf(TenantVerificationException.class)
                .hasMessageContaining("The tenant identifier must contain only alphanumeric characters");
        });

        assertThat(invocations).hasValue(0);
        assertThat(tenantDataSource.getCreatedTenantIdentifiers()).isEmpty();
    }

    @Test
    void whenCustomTenantIdentifierValidatorThenApplied() {
        var tenantDataSource = TenantDataSource.builder()
            .dataSourceFactory(TestDataSource::new)
            .tenantIdentifierValidator(identifier -> {
                throw new TenantVerificationException("Tenants must be onboarded first");
            })
            .build();

        TenantContext.where("acme").run(() -> {
            assertThatThrownBy(tenantDataSource::getConnection)
                .isInstanceOf(TenantVerificationException.class)
                .hasMessageContaining("Tenants must be onboarded first");
        });
    }

    @Test
    void whenMaximumNumberOfDataSourcesReachedThenReject() throws Exception {
        var tenantDataSource = TenantDataSource.builder()
            .dataSourceFactory(TestDataSource::new)
            .maxTenantDataSources(2)
            .build();

        TenantContext.where("one").call(tenantDataSource::getConnection);
        TenantContext.where("two").call(tenantDataSource::getConnection);

        TenantContext.where("three").run(() -> {
            assertThatThrownBy(tenantDataSource::getConnection)
                .isInstanceOf(TenantNotFoundException.class)
                .hasMessageContaining("the maximum number of tenant data sources (2) has been reached");
        });

        assertThat(tenantDataSource.getCreatedTenantIdentifiers()).containsExactlyInAnyOrder("one", "two");
    }

    @Test
    void whenMaximumNumberOfDataSourcesReachedThenKnownTenantsStillResolve() throws Exception {
        var tenantDataSource = TenantDataSource.builder()
            .dataSourceFactory(TestDataSource::new)
            .maxTenantDataSources(1)
            .build();

        TenantContext.where("one").call(tenantDataSource::getConnection);

        TenantContext.where("one").run(() -> assertThatNoException().isThrownBy(tenantDataSource::getConnection));
    }

    @Test
    void whenMaximumNumberOfDataSourcesIsNotPositiveThenThrow() {
        assertThatThrownBy(() -> TenantDataSource.builder().maxTenantDataSources(0).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("maxTenantDataSources must be greater than zero");
    }

    @Test
    void whenDataSourcesRegisteredUpfrontThenTheyDoNotCountTowardsTheMaximum() throws Exception {
        var tenantDataSource = TenantDataSource.builder()
            .dataSource("acme", acme)
            .dataSource("beans", beans)
            .dataSourceFactory(TestDataSource::new)
            .maxTenantDataSources(1)
            .build();

        TenantContext.where("acme").call(tenantDataSource::getConnection);
        TenantContext.where("beans").call(tenantDataSource::getConnection);
        TenantContext.where("created").call(tenantDataSource::getConnection);

        assertThat(tenantDataSource.getCreatedTenantIdentifiers()).containsExactly("created");
    }

    @Test
    void whenClosingOneDataSourceFailsThenTheOthersAreStillClosed() throws Exception {
        var failing = new TestDataSource("failing") {
            @Override
            public void close() {
                throw new IllegalStateException("cannot close");
            }
        };
        var healthy = new TestDataSource("healthy");
        var tenantDataSource = TenantDataSource.builder()
            .dataSourceFactory(identifier -> "failing".equals(identifier) ? failing : healthy)
            .build();

        TenantContext.where("failing").call(tenantDataSource::getConnection);
        TenantContext.where("healthy").call(tenantDataSource::getConnection);

        assertThatThrownBy(tenantDataSource::destroy).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("cannot close");

        assertThat(healthy.closed).isTrue();
        assertThat(tenantDataSource.getCreatedTenantIdentifiers()).isEmpty();
    }

    @Test
    void createdTenantIdentifiersAreNotModifiable() throws Exception {
        var tenantDataSource = TenantDataSource.builder().dataSourceFactory(TestDataSource::new).build();

        TenantContext.where("acme").call(tenantDataSource::getConnection);

        assertThatThrownBy(() -> tenantDataSource.getCreatedTenantIdentifiers().add("beans"))
            .isInstanceOf(UnsupportedOperationException.class);
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
