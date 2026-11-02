package io.arconia.multitenancy.details.jdbc.autoconfigure;

import java.util.List;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import io.arconia.multitenancy.core.autoconfigure.MultitenancyCoreAutoConfiguration;
import io.arconia.multitenancy.core.resolution.TenantVerifier;
import io.arconia.multitenancy.core.tenantdetails.TenantDetails;
import io.arconia.multitenancy.core.tenantdetails.TenantDetailsService;
import io.arconia.multitenancy.details.jdbc.JdbcTenantDetailsService;

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
            assertThat(context).hasSingleBean(TenantDetailsService.class);
            assertThat(context).hasSingleBean(JdbcTenantDetailsService.class);
        });
    }

    @Test
    void tenantDetailsServiceWhenDisabled() {
        contextRunner.withPropertyValues("arconia.multitenancy.details.jdbc.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(TenantDetailsService.class);
            assertThat(context).doesNotHaveBean(JdbcTenantDetailsDataSourceScriptDatabaseInitializer.class);
        });
    }

    @Test
    void tenantDetailsServiceWhenNoDataSource() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(JdbcTenantDetailsAutoConfiguration.class))
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(TenantDetailsService.class);
                assertThat(context).doesNotHaveBean(JdbcTenantDetailsDataSourceScriptDatabaseInitializer.class);
            });
    }

    @Test
    void tenantDetailsServiceWhenCustom() {
        contextRunner.withUserConfiguration(CustomTenantDetailsServiceConfiguration.class).run(context -> {
            assertThat(context).hasSingleBean(TenantDetailsService.class);
            assertThat(context).doesNotHaveBean(JdbcTenantDetailsService.class);
        });
    }

    @Test
    void tenantDetailsServiceWhenTenantsConfigured() {
        contextRunner.withConfiguration(AutoConfigurations.of(MultitenancyCoreAutoConfiguration.class))
            .withPropertyValues("arconia.multitenancy.details.tenants[0].identifier=acme")
            .run(context -> {
                assertThat(context).hasSingleBean(TenantDetailsService.class);
                assertThat(context).hasSingleBean(JdbcTenantDetailsService.class);
            });
    }

    @Test
    void tenantDetailsServiceWhenTenantsConfiguredAndDisabled() {
        contextRunner.withConfiguration(AutoConfigurations.of(MultitenancyCoreAutoConfiguration.class))
            .withPropertyValues("arconia.multitenancy.details.jdbc.enabled=false",
                    "arconia.multitenancy.details.tenants[0].identifier=acme")
            .run(context -> {
                assertThat(context).hasSingleBean(TenantDetailsService.class);
                assertThat(context).doesNotHaveBean(JdbcTenantDetailsService.class);
            });
    }

    @Test
    void tenantDetailsServiceWhenJdbcClientBeanThenUsed() {
        contextRunner.withUserConfiguration(JdbcClientConfiguration.class).run(context -> {
            var jdbcClient = context.getBean(JdbcClient.class);

            context.getBean(JdbcTenantDetailsService.class).loadTenantByIdentifier("acme");

            Mockito.verify(jdbcClient).sql(Mockito.anyString());
        });
    }

    @Test
    void tenantDetailsServiceWhenQualifiedDataSourceAmongSeveralThenItIsUsed() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(JdbcTenantDetailsAutoConfiguration.class))
            .withUserConfiguration(QualifiedDataSourceConfiguration.class)
            .run(context -> {
                assertThat(context).hasSingleBean(JdbcTenantDetailsService.class);
                assertThat(context.getBean(JdbcTenantDetailsService.class).loadAllTenants()).isEmpty();
                assertThat(hasTenantDetailsTable(context.getBean("tenantDetailsDataSource", DataSource.class))).isTrue();
                assertThat(hasTenantDetailsTable(context.getBean("businessDataSource", DataSource.class))).isFalse();
            });
    }

    @Test
    void tenantDetailsServiceWhenQualifiedDataSourceAndPrimaryThenQualifiedIsUsed() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(JdbcTenantDetailsAutoConfiguration.class))
            .withUserConfiguration(QualifiedAndPrimaryDataSourceConfiguration.class)
            .run(context -> {
                assertThat(hasTenantDetailsTable(context.getBean("tenantDetailsDataSource", DataSource.class))).isTrue();
                assertThat(hasTenantDetailsTable(context.getBean("businessDataSource", DataSource.class))).isFalse();
            });
    }

    @Test
    void tenantDetailsServiceWhenQualifiedDataSourceIsNotDefaultCandidateThenItIsUsedAlongsideAutoConfiguredOne() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class, JdbcTenantDetailsAutoConfiguration.class))
            .withPropertyValues("spring.datasource.generate-unique-name=true")
            .withUserConfiguration(NonDefaultCandidateDataSourceConfiguration.class)
            .run(context -> {
                assertThat(context).hasSingleBean(JdbcTenantDetailsService.class);
                var mainDataSource = context.getBean(DataSource.class);
                var tenantDetailsDataSource = context.getBean("tenantDetailsDataSource", DataSource.class);
                assertThat(mainDataSource).isNotSameAs(tenantDetailsDataSource);
                assertThat(hasTenantDetailsTable(tenantDetailsDataSource)).isTrue();
                assertThat(hasTenantDetailsTable(mainDataSource)).isFalse();
            });
    }

    @Test
    void tenantDetailsServiceWhenSeveralDataSourcesWithoutQualifierThenBacksOff() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(JdbcTenantDetailsAutoConfiguration.class))
            .withUserConfiguration(SeveralDataSourcesConfiguration.class)
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(TenantDetailsService.class);
            });
    }

    private static boolean hasTenantDetailsTable(DataSource dataSource) {
        try {
            new JdbcTemplate(dataSource).queryForObject("select count(*) from tenant_details", Integer.class);
            return true;
        }
        catch (DataAccessException ex) {
            return false;
        }
    }

    private static DataSource embeddedDataSource() {
        return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).generateUniqueName(true).build();
    }

    @Test
    void tenantVerifierWhenJdbc() {
        contextRunner.withConfiguration(AutoConfigurations.of(MultitenancyCoreAutoConfiguration.class))
            .run(context -> {
                assertThat(context).hasSingleBean(JdbcTenantDetailsService.class);
                assertThat(context).hasSingleBean(TenantVerifier.class);
            });
    }

    @Test
    void databaseInitializerWhenEmbeddedDataSource() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(JdbcTenantDetailsDataSourceScriptDatabaseInitializer.class);
            assertThat(context.getBean(JdbcTenantDetailsService.class).loadAllTenants()).isEmpty();
        });
    }

    @Test
    void databaseInitializerWhenNever() {
        contextRunner.withPropertyValues("arconia.multitenancy.details.jdbc.initialize-schema=never").run(context -> {
            assertThat(context).doesNotHaveBean(JdbcTenantDetailsDataSourceScriptDatabaseInitializer.class);
        });
    }

    @Test
    void databaseInitializerWhenCustom() {
        contextRunner.withUserConfiguration(CustomDatabaseInitializerConfiguration.class).run(context -> {
            assertThat(context).hasSingleBean(JdbcTenantDetailsDataSourceScriptDatabaseInitializer.class);
            assertThat(context).hasBean("customDatabaseInitializer");
        });
    }

    @Test
    void databaseInitializerWhenSchemaLocationUnknown() {
        contextRunner.withPropertyValues("arconia.multitenancy.details.jdbc.schema=classpath:unknown-schema.sql")
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
    static class QualifiedDataSourceConfiguration {

        @Bean
        DataSource businessDataSource() {
            return embeddedDataSource();
        }

        @Bean
        @TenantDetailsDataSource
        DataSource tenantDetailsDataSource() {
            return embeddedDataSource();
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class QualifiedAndPrimaryDataSourceConfiguration {

        @Bean
        @Primary
        DataSource businessDataSource() {
            return embeddedDataSource();
        }

        @Bean
        @TenantDetailsDataSource
        DataSource tenantDetailsDataSource() {
            return embeddedDataSource();
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class NonDefaultCandidateDataSourceConfiguration {

        @Bean(defaultCandidate = false)
        @TenantDetailsDataSource
        DataSource tenantDetailsDataSource() {
            return embeddedDataSource();
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class SeveralDataSourcesConfiguration {

        @Bean
        DataSource businessDataSource() {
            return embeddedDataSource();
        }

        @Bean
        DataSource otherDataSource() {
            return embeddedDataSource();
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class JdbcClientConfiguration {

        @Bean
        JdbcClient jdbcClient() {
            return Mockito.mock(JdbcClient.class, Answers.RETURNS_DEEP_STUBS);
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
