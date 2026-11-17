package io.arconia.data.jdbc.sqlite.support;

import java.sql.SQLException;

import org.jspecify.annotations.Nullable;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.support.AbstractFallbackSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionSubclassTranslator;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

import io.arconia.core.annotations.Incubating;

/**
 * {@link org.springframework.jdbc.support.SQLExceptionTranslator} for SQLite.
 * <p>
 * The SQLite JDBC driver reports errors without an SQL state and with the primary SQLite result
 * code only, so Spring's default translation can't categorize them. This translator reads the
 * extended result code instead:
 * <ul>
 * <li>A violated {@code UNIQUE} or {@code PRIMARY KEY} constraint becomes a {@link DuplicateKeyException}.</li>
 * <li>Any other violated constraint becomes a {@link DataIntegrityViolationException}.</li>
 * <li>A database that is busy or locked by another connection becomes a {@link CannotAcquireLockException}.</li>
 * </ul>
 * Other errors are handed to the fallback translator, a {@link SQLExceptionSubclassTranslator} by default.
 *
 * @see <a href="https://www.sqlite.org/rescode.html">SQLite Result Codes</a>
 */
@Incubating
public class SqliteExceptionTranslator extends AbstractFallbackSQLExceptionTranslator {

    private static final int PRIMARY_RESULT_CODE_MASK = 0xFF;

    public SqliteExceptionTranslator() {
        setFallbackTranslator(new SQLExceptionSubclassTranslator());
    }

    @Override
    protected @Nullable DataAccessException doTranslate(String task, @Nullable String sql, SQLException ex) {
        if (!(ex instanceof SQLiteException sqliteException)) {
            return null;
        }
        SQLiteErrorCode resultCode = sqliteException.getResultCode();
        if (resultCode == SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE
                || resultCode == SQLiteErrorCode.SQLITE_CONSTRAINT_PRIMARYKEY) {
            return new DuplicateKeyException(buildMessage(task, sql, ex), ex);
        }
        int primaryResultCode = resultCode.code & PRIMARY_RESULT_CODE_MASK;
        if (primaryResultCode == SQLiteErrorCode.SQLITE_CONSTRAINT.code) {
            return new DataIntegrityViolationException(buildMessage(task, sql, ex), ex);
        }
        if (primaryResultCode == SQLiteErrorCode.SQLITE_BUSY.code || primaryResultCode == SQLiteErrorCode.SQLITE_LOCKED.code) {
            return new CannotAcquireLockException(buildMessage(task, sql, ex), ex);
        }
        return null;
    }

}
