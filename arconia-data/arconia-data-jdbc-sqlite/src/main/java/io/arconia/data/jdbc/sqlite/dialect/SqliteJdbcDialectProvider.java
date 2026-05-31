package io.arconia.data.jdbc.sqlite.dialect;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.data.jdbc.core.dialect.DialectResolver;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.jdbc.core.JdbcOperations;

/**
 * JDBC Dialect discovery mechanism for SQLite.
 */
class SqliteJdbcDialectProvider implements DialectResolver.JdbcDialectProvider {

    @Override
    public Optional<Dialect> getDialect(JdbcOperations operations) {
        return Optional.ofNullable(operations.execute(this::getSqliteDialect));
    }

    @Nullable
    private Dialect getSqliteDialect(Connection connection) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        String name = metadata.getDatabaseProductName().toLowerCase(Locale.ENGLISH);
        return name.contains("sqlite") ? JdbcSqliteDialect.INSTANCE : null;
    }

}
