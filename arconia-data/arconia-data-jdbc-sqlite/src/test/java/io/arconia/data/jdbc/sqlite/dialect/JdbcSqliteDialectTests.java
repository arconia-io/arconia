package io.arconia.data.jdbc.sqlite.dialect;

import org.junit.jupiter.api.Test;
import org.springframework.data.relational.core.sql.IdentifierProcessing;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JdbcSqliteDialect}.
 * <p>
 * The SQL the dialect renders and the converters it contributes are covered against a real
 * database by {@link JdbcSqliteDialectIT}.
 */
class JdbcSqliteDialectTests {

    private final JdbcSqliteDialect dialect = JdbcSqliteDialect.INSTANCE;

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
        assertThat(dialect.getIdGeneration().sequencesSupported()).isFalse();
    }

    @Test
    void shouldNotSupportSingleQueryLoading() {
        assertThat(dialect.supportsSingleQueryLoading()).isFalse();
    }

}
