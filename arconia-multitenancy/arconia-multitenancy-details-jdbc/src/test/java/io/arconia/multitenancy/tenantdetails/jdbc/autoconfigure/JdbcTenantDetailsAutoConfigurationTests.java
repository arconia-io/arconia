package io.arconia.multitenancy.tenantdetails.jdbc.autoconfigure;

import java.util.List;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import io.arconia.multitenancy.core.autoconfigure.MultitenancyCoreAutoConfiguration;
import io.arconia.multitenancy.core.tenantdetails.TenantDetails;
import io.arconia.multitenancy.core.tenantdetails.TenantDetailsService;
import io.arconia.multitenancy.core.tenantdetails.TenantVerifier;
import io.arconia.multitenancy.tenantdetails.jdbc.JdbcTenantDetailsService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JdbcTenantDetailsAutoConfiguration}.
 */
class JdbcTenantDetailsAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(JdbcTenantDetailsAutoConfiguration.class))
        .withUserConfiguration(EmbeddedDataSourceConfiguration.class);

    @Test
    void tenantDetailsServiceWhenDefault() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(TenantDetailsService.class);
            assertThat(context).doesNotHaveBean(JdbcTenantDetailsDataSourceScriptDatabaseInitializer.class);
        });
    }

    @Test
    void tenantDetailsServiceWhenJdbc() {
        contextRunner.withPropertyValues("arconia.multitenancy.details.source=jdbc").run(context -> {
            assertThat(context).hasSingleBean(TenantDetailsService.class);
            assertThat(context).hasSingleBean(JdbcTenantDetailsService.class);
        });
    }

    @Test
    void tenantDetailsServiceWhenNoDataSource() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(JdbcTenantDetailsAutoConfiguration.class))
            .withPropertyValues("arconia.multitenancy.details.source=jdbc")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(TenantDetailsService.class);
                assertThat(context).doesNotHaveBean(JdbcTenantDetailsDataSourceScriptDatabaseInitializer.class);
            });
    }

    @Test
    void tenantDetailsServiceWhenCustom() {
        contextRunner.withPropertyValues("arconia.multitenancy.details.source=jdbc")
            .withUserConfiguration(CustomTenantDetailsServiceConfiguration.class)
            .run(context -> {
                assertThat(context).hasSingleBean(TenantDetailsService.class);
                assertThat(context).doesNotHaveBean(JdbcTenantDetailsService.class);
            });
    }

    @Test
    void tenantVerifierWhenJdbc() {
        contextRunner
            .withConfiguration(AutoConfigurations.of(MultitenancyCoreAutoConfiguration.class))
            .withPropertyValues("arconia.multitenancy.details.source=jdbc")
            .run(context -> {
                assertThat(context).hasSingleBean(JdbcTenantDetailsService.class);
                assertThat(context).hasSingleBean(TenantVerifier.class);
            });
    }

    @Test
    void databaseInitializerWhenEmbeddedDataSource() {
        contextRunner.withPropertyValues("arconia.multitenancy.details.source=jdbc").run(context -> {
            assertThat(context).hasSingleBean(JdbcTenantDetailsDataSourceScriptDatabaseInitializer.class);
            assertThat(context.getBean(JdbcTenantDetailsService.class).loadAllTenants()).isEmpty();
        });
    }

    @Test
    void databaseInitializerWhenNever() {
        contextRunner
            .withPropertyValues("arconia.multitenancy.details.source=jdbc",
                    "arconia.multitenancy.details.jdbc.initialize-schema=never")
            .run(context -> {
                assertThat(context).doesNotHaveBean(JdbcTenantDetailsDataSourceScriptDatabaseInitializer.class);
            });
    }

    @Test
    void databaseInitializerWhenCustom() {
        contextRunner.withPropertyValues("arconia.multitenancy.details.source=jdbc")
            .withUserConfiguration(CustomDatabaseInitializerConfiguration.class)
            .run(context -> {
                assertThat(context).hasSingleBean(JdbcTenantDetailsDataSourceScriptDatabaseInitializer.class);
                assertThat(context).hasBean("customDatabaseInitializer");
            });
    }

    @Test
    void databaseInitializerWhenSchemaLocationUnknown() {
        contextRunner
            .withPropertyValues("arconia.multitenancy.details.source=jdbc",
                    "arconia.multitenancy.details.jdbc.schema=classpath:unknown-schema.sql")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context).getFailure().hasMessageContaining("No schema scripts found at location");
            });
    }

    @Configuration(proxyBeanMethods = false)
    static class EmbeddedDataSourceConfiguration {

        @Bean
        DataSource dataSource() {
            return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).generateUniqueName(true).build();
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class CustomTenantDetailsServiceConfiguration {

        @Bean
        TenantDetailsService customTenantDetailsService() {
            return new TenantDetailsService() {

                @Override
                public List<? extends TenantDetails> loadAllTenants() {
                    return List.of();
                }

                @Override
                public @Nullable TenantDetails loadTenantByIdentifier(String identifier) {
                    return null;
                }

            };
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class CustomDatabaseInitializerConfiguration {

        @Bean
        JdbcTenantDetailsDataSourceScriptDatabaseInitializer customDatabaseInitializer(DataSource dataSource,
                JdbcTenantDetailsProperties properties) {
            return new JdbcTenantDetailsDataSourceScriptDatabaseInitializer(dataSource, properties);
        }

    }

}
