package io.arconia.data.jdbc.sqlite.dialect;

import org.junit.jupiter.api.Test;
import org.springframework.data.relational.core.dialect.IdGeneration;
import org.springframework.data.relational.core.dialect.LimitClause;
import org.springframework.data.relational.core.dialect.LockClause;
import org.springframework.data.relational.core.sql.IdentifierProcessing;
import org.springframework.data.relational.core.sql.LockMode;
import org.springframework.data.relational.core.sql.LockOptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link SqliteDialect}.
 */
class SqliteDialectTests {

    private final SqliteDialect dialect = JdbcSqliteDialect.INSTANCE;

    @Test
    void shouldRenderLimit() {
        LimitClause limit = dialect.limit();

        assertThat(limit.getClausePosition()).isEqualTo(LimitClause.Position.AFTER_ORDER_BY);
        assertThat(limit.getLimit(10)).isEqualTo("LIMIT 10");
    }

    @Test
    void shouldRenderOffset() {
        LimitClause limit = dialect.limit();

        assertThat(limit.getOffset(10)).isEqualTo("OFFSET 10");
    }

    @Test
    void shouldRenderLimitOffset() {
        LimitClause limit = dialect.limit();

        assertThat(limit.getLimitOffset(20, 10)).isEqualTo("LIMIT 20 OFFSET 10");
    }

    @Test
    void shouldEmitEmptyLockClause() {
        LockClause lock = dialect.lock();
        LockOptions options = new LockOptions(LockMode.PESSIMISTIC_WRITE, mock());

        assertThat(lock.getLock(options)).isEmpty();
        assertThat(lock.getClausePosition()).isEqualTo(LockClause.Position.AFTER_ORDER_BY);
    }

    @Test
    void shouldQuoteIdentifiersUsingDoubleQuotes() {
        IdentifierProcessing identifierProcessing = dialect.getIdentifierProcessing();

        assertThat(identifierProcessing.quote("abc")).isEqualTo("\"abc\"");
    }

    @Test
    void shouldPreserveIdentifierCasing() {
        IdentifierProcessing identifierProcessing = dialect.getIdentifierProcessing();

        assertThat(identifierProcessing.standardizeLetterCase("MixedCase")).isEqualTo("MixedCase");
    }

    @Test
    void shouldNotSupportSequences() {
        IdGeneration idGeneration = dialect.getIdGeneration();

        assertThat(idGeneration.sequencesSupported()).isFalse();
    }

    @Test
    void shouldNotSupportBatchIdGeneration() {
        IdGeneration idGeneration = dialect.getIdGeneration();

        assertThat(idGeneration.supportedForBatchOperations()).isFalse();
    }

    @Test
    void shouldNotSupportSingleQueryLoading() {
        assertThat(dialect.supportsSingleQueryLoading()).isFalse();
    }

    @Test
    void shouldRegisterReadingConverters() {
        assertThat(dialect.getConverters())
                .containsExactlyInAnyOrder(
                        NumberToBooleanConverter.INSTANCE,
                        TimestampAtUtcToOffsetDateTimeConverter.INSTANCE);
    }

}
