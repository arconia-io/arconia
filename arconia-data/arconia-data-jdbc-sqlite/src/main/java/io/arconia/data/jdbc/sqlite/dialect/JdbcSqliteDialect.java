package io.arconia.data.jdbc.sqlite.dialect;

import org.springframework.data.jdbc.core.dialect.JdbcDialect;

/**
 * Spring Data JDBC-specific dialect for SQLite.
 */
public class JdbcSqliteDialect extends SqliteDialect implements JdbcDialect {

    public static final JdbcSqliteDialect INSTANCE = new JdbcSqliteDialect();

    private JdbcSqliteDialect() {}

}
