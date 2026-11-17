package io.arconia.data.jdbc.sqlite.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.transaction.autoconfigure.TransactionManagerCustomizationAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;

import io.arconia.data.jdbc.sqlite.support.SqliteExceptionTranslator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JdbcSqliteAutoConfiguration}.
 */
class JdbcSqliteAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JdbcSqliteAutoConfiguration.class, DataSourceAutoConfiguration.class,
                    JdbcTemplateAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class,
                    TransactionManagerCustomizationAutoConfiguration.class))
            .withPropertyValues("spring.datasource.url=jdbc:sqlite::memory:");

    @Test
    void exceptionTranslatorAppliedToJdbcTemplateAndTransactionManager() {
        contextRunner.run(context -> {
            SqliteExceptionTranslator exceptionTranslator = context.getBean(SqliteExceptionTranslator.class);
            assertThat(context.getBean(JdbcTemplate.class).getExceptionTranslator()).isSameAs(exceptionTranslator);
            assertThat(context.getBean(JdbcTransactionManager.class).getExceptionTranslator()).isSameAs(exceptionTranslator);
        });
    }

    @Test
    void exceptionTranslatorWhenCustom() {
        contextRunner.withUserConfiguration(CustomExceptionTranslatorConfiguration.class).run(context -> {
            assertThat(context).doesNotHaveBean(SqliteExceptionTranslator.class);
            assertThat(context.getBean(JdbcTemplate.class).getExceptionTranslator())
                    .isSameAs(context.getBean(SQLExceptionTranslator.class));
            assertThat(context.getBean(JdbcTransactionManager.class).getExceptionTranslator())
                    .isNotInstanceOf(SqliteExceptionTranslator.class);
        });
    }

    @Test
    void exceptionTranslatorWhenNoDataSource() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(JdbcSqliteAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(SqliteExceptionTranslator.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomExceptionTranslatorConfiguration {

        @Bean
        SQLExceptionTranslator customExceptionTranslator() {
            return new SQLStateSQLExceptionTranslator();
        }

    }

}
