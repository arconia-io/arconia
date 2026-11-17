package io.arconia.data.jdbc.sqlite.support;

import java.sql.SQLIntegrityConstraintViolationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link SqliteExceptionTranslator}.
 */
class SqliteExceptionTranslatorTests {

    private final SqliteExceptionTranslator translator = new SqliteExceptionTranslator();

    @ParameterizedTest
    @EnumSource(names = { "SQLITE_CONSTRAINT_UNIQUE", "SQLITE_CONSTRAINT_PRIMARYKEY" })
    void shouldTranslateDuplicateKey(SQLiteErrorCode resultCode) {
        assertThat(translator.translate("insert", "INSERT", new SQLiteException("duplicate", resultCode)))
                .isExactlyInstanceOf(DuplicateKeyException.class);
    }

    @ParameterizedTest
    @EnumSource(names = { "SQLITE_CONSTRAINT", "SQLITE_CONSTRAINT_NOTNULL", "SQLITE_CONSTRAINT_FOREIGNKEY",
            "SQLITE_CONSTRAINT_CHECK" })
    void shouldTranslateOtherConstraintViolations(SQLiteErrorCode resultCode) {
        assertThat(translator.translate("insert", "INSERT", new SQLiteException("violation", resultCode)))
                .isExactlyInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @EnumSource(names = { "SQLITE_BUSY", "SQLITE_BUSY_SNAPSHOT", "SQLITE_LOCKED", "SQLITE_LOCKED_SHAREDCACHE" })
    void shouldTranslateBusyAndLocked(SQLiteErrorCode resultCode) {
        assertThat(translator.translate("update", "UPDATE", new SQLiteException("locked", resultCode)))
                .isExactlyInstanceOf(CannotAcquireLockException.class);
    }

    @Test
    void shouldKeepSqlAndCauseInTranslatedException() {
        SQLiteException ex = new SQLiteException("duplicate", SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE);

        assertThat(translator.translate("insert", "INSERT INTO book", ex))
                .hasMessageContaining("INSERT INTO book")
                .hasCause(ex);
    }

    @Test
    void shouldDelegateOtherErrorsToFallback() {
        SQLExceptionTranslator fallback = mock();
        translator.setFallbackTranslator(fallback);
        SQLiteException ex = new SQLiteException("error", SQLiteErrorCode.SQLITE_ERROR);

        translator.translate("query", "SELECT", ex);

        verify(fallback).translate("query", "SELECT", ex);
    }

    @Test
    void shouldTranslateOtherDriverExceptionsByDefault() {
        assertThat(translator.translate("insert", "INSERT", new SQLIntegrityConstraintViolationException()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

}
