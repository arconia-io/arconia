package io.arconia.data.jdbc.sqlite.dialect;

import java.util.Collection;
import java.util.List;

import org.springframework.data.relational.core.dialect.AbstractDialect;
import org.springframework.data.relational.core.dialect.IdGeneration;
import org.springframework.data.relational.core.dialect.LimitClause;
import org.springframework.data.relational.core.dialect.LockClause;
import org.springframework.data.relational.core.sql.IdentifierProcessing;
import org.springframework.data.relational.core.sql.LockOptions;

/**
 * An SQL dialect for SQLite.
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

        @Override
        public String getOffset(long offset) {
            return "OFFSET %d".formatted(offset);
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
     * Sqlite uses file-level locking, so no SQL lock syntax is emitted.
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
     * Sqlite does not support sequences. Identity is managed via ROWID internally.
     * Batch inserts are disabled because the Sqlite JDBC driver does not return
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
        return List.of(
                NumberToBooleanConverter.INSTANCE,
                TimestampAtUtcToOffsetDateTimeConverter.INSTANCE
        );
    }

    /**
     * Sqlite does not support the LATERAL joins that single-query loading relies on.
     */
    @Override
    public boolean supportsSingleQueryLoading() {
        return false;
    }

}
