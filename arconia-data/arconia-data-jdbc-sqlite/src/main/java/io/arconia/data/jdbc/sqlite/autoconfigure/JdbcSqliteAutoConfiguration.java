package io.arconia.data.jdbc.sqlite.autoconfigure;

import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.boot.transaction.autoconfigure.TransactionManagerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;

import io.arconia.data.jdbc.sqlite.support.SqliteExceptionTranslator;

/**
 * Auto-configuration for SQLite with Spring JDBC.
 */
@AutoConfiguration(after = DataSourceAutoConfiguration.class,
        before = { JdbcTemplateAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class })
@ConditionalOnSingleCandidate(DataSource.class)
public final class JdbcSqliteAutoConfiguration {

    /**
     * Spring Boot applies the translator to the auto-configured {@code JdbcTemplate}, which
     * Spring Data JDBC and {@code JdbcClient} use too. Errors from other databases are translated
     * as they are by default.
     */
    @Bean
    @ConditionalOnMissingBean(SQLExceptionTranslator.class)
    SqliteExceptionTranslator sqliteExceptionTranslator(DataSource dataSource) {
        SqliteExceptionTranslator exceptionTranslator = new SqliteExceptionTranslator();
        exceptionTranslator.setFallbackTranslator(new SQLErrorCodeSQLExceptionTranslator(dataSource));
        return exceptionTranslator;
    }

    /**
     * Applies the translator to errors raised when committing or rolling back, such as a
     * violated deferred foreign key constraint.
     */
    @Bean
    TransactionManagerCustomizer<JdbcTransactionManager> sqliteTransactionManagerCustomizer(
            ObjectProvider<SqliteExceptionTranslator> exceptionTranslator) {
        return transactionManager -> exceptionTranslator.ifUnique(transactionManager::setExceptionTranslator);
    }

}
