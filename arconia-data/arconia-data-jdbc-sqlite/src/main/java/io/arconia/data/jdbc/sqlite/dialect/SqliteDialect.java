package io.arconia.data.jdbc.sqlite.dialect;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.data.relational.core.dialect.AbstractDialect;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.dialect.IdGeneration;
import org.springframework.data.relational.core.dialect.LimitClause;
import org.springframework.data.relational.core.dialect.LockClause;
import org.springframework.data.relational.core.sql.IdentifierProcessing;
import org.springframework.data.relational.core.sql.LockOptions;
import org.springframework.data.relational.core.sql.render.UpsertRenderContext;
import org.springframework.data.relational.core.sql.render.UpsertRenderContexts;

/**
 * SQL {@link Dialect} for SQLite.
 */
public class SqliteDialect extends AbstractDialect {

    protected SqliteDialect() {}

    private static final IdentifierProcessing IDENTIFIER_PROCESSING =
            IdentifierProcessing.create(IdentifierProcessing.Quoting.ANSI, IdentifierProcessing.LetterCasing.AS_IS);

    private static final LimitClause LIMIT_CLAUSE = new LimitClause() {
        @Override
        public String getLimit(long limit) {
            return "LIMIT %d".formatted(limit);
        }

        /**
         * SQLite accepts {@code OFFSET} only after {@code LIMIT}, where a negative limit means no limit.
         */
        @Override
        public String getOffset(long offset) {
            return "LIMIT -1 OFFSET %d".formatted(offset);
        }

        @Override
        public String getLimitOffset(long limit, long offset) {
            return "LIMIT %d OFFSET %d".formatted(limit, offset);
        }

        @Override
        public Position getClausePosition() {
            return Position.AFTER_ORDER_BY;
        }
    };

    /**
     * SQLite uses file-level locking, so no SQL lock clause is emitted.
     */
    private static final LockClause LOCK_CLAUSE = new LockClause() {
        @Override
        public String getLock(LockOptions lockOptions) {
            return "";
        }

        @Override
        public Position getClausePosition() {
            return Position.AFTER_ORDER_BY;
        }
    };

    /**
     * SQLite manages identity internally via {@code ROWID} and does not support sequences.
     * Batch ID generation is disabled because the SQLite JDBC driver does not return
     * generated keys from batch statement executions.
     */
    private static final IdGeneration ID_GENERATION = new IdGeneration() {
        @Override
        public boolean sequencesSupported() {
            return false;
        }

        @Override
        public boolean supportedForBatchOperations() {
            return false;
        }
    };

    private static final List<Object> CONVERTERS = Stream.concat(
            Stream.of(NumberToBooleanConverter.INSTANCE),
            TemporalConverters.converters().stream()
    ).toList();

    @Override
    public LimitClause limit() {
        return LIMIT_CLAUSE;
    }

    @Override
    public LockClause lock() {
        return LOCK_CLAUSE;
    }

    @Override
    public IdentifierProcessing getIdentifierProcessing() {
        return IDENTIFIER_PROCESSING;
    }

    @Override
    public IdGeneration getIdGeneration() {
        return ID_GENERATION;
    }

    @Override
    public Collection<Object> getConverters() {
        return CONVERTERS;
    }

    /**
     * SQLite does not support the {@code LATERAL} joins that single-query loading relies on.
     */
    @Override
    public boolean supportsSingleQueryLoading() {
        return false;
    }

    /**
     * SQLite does not support the {@code MERGE} statement that Spring Data falls back to for
     * upserts, and Spring Data does not allow dialects to contribute their own upsert syntax.
     *
     * @see <a href="https://github.com/spring-projects/spring-data-relational/issues/2304">spring-data-relational#2304</a>
     */
    @Override
    public UpsertRenderContext getUpsertRenderContext() {
        return UpsertRenderContexts.UNSUPPORTED;
    }

}
