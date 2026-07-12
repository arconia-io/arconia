package io.arconia.data.jdbc.sqlite.dialect;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SqliteJdbcDialectProvider}.
 */
class SqliteJdbcDialectProviderTests {

    private final SqliteJdbcDialectProvider provider = new SqliteJdbcDialectProvider();

    @Test
    void shouldResolveDialectForSqliteDatabase() {
        Optional<Dialect> dialect = provider.getDialect(operationsForProduct("SQLite"));

        assertThat(dialect).contains(JdbcSqliteDialect.INSTANCE);
    }

    @Test
    void shouldResolveDialectIgnoringProductNameCase() {
        Optional<Dialect> dialect = provider.getDialect(operationsForProduct("sqlite"));

        assertThat(dialect).contains(JdbcSqliteDialect.INSTANCE);
    }

    @Test
    void shouldResolveDialectWhenProductNameContainsSqlite() {
        Optional<Dialect> dialect = provider.getDialect(operationsForProduct("SQLite 3"));

        assertThat(dialect).contains(JdbcSqliteDialect.INSTANCE);
    }

    @Test
    void shouldReturnEmptyForOtherDatabase() {
        Optional<Dialect> dialect = provider.getDialect(operationsForProduct("PostgreSQL"));

        assertThat(dialect).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static JdbcOperations operationsForProduct(String productName) {
        JdbcOperations operations = mock(JdbcOperations.class);
        when(operations.execute(any(ConnectionCallback.class))).thenAnswer(invocation -> {
            ConnectionCallback<Dialect> callback = invocation.getArgument(0);
            Connection connection = mock(Connection.class);
            DatabaseMetaData metaData = mock(DatabaseMetaData.class);
            when(connection.getMetaData()).thenReturn(metaData);
            when(metaData.getDatabaseProductName()).thenReturn(productName);
            return callback.doInConnection(connection);
        });
        return operations;
    }

}
