package io.arconia.data.jdbc.sqlite.dialect;

import org.springframework.data.jdbc.core.dialect.JdbcDialect;

/**
 * {@link JdbcDialect} for SQLite.
 *
 * <p>Use {@link #INSTANCE} to reference the dialect, for example, when declaring it as a bean
 * to support ahead-of-time processing.
 */
public class JdbcSqliteDialect extends SqliteDialect implements JdbcDialect {

    public static final JdbcSqliteDialect INSTANCE = new JdbcSqliteDialect();

    private JdbcSqliteDialect() {}

}
