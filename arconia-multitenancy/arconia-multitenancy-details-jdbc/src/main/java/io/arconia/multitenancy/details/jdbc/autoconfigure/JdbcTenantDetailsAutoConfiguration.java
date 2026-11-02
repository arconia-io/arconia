package io.arconia.multitenancy.details.jdbc.autoconfigure;

import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcClientAutoConfiguration;
import org.springframework.boot.sql.autoconfigure.init.OnDatabaseInitializationCondition;
import org.springframework.boot.sql.init.dependency.DatabaseInitializationDependencyConfigurer;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import io.arconia.multitenancy.core.autoconfigure.MultitenancyCoreAutoConfiguration;
import io.arconia.multitenancy.core.tenantdetails.TenantDetailsService;
import io.arconia.multitenancy.details.jdbc.JdbcTenantDetailsService;

/**
 * Auto-configuration for JDBC-based tenant details.
 */
@AutoConfiguration(after = { DataSourceAutoConfiguration.class, JdbcClientAutoConfiguration.class },
        before = MultitenancyCoreAutoConfiguration.class)
@Conditional(JdbcTenantDetailsAutoConfiguration.OnTenantDetailsDataSourceCondition.class)
@ConditionalOnBooleanProperty(prefix = JdbcTenantDetailsProperties.CONFIG_PREFIX, value = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(JdbcTenantDetailsProperties.class)
public final class JdbcTenantDetailsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TenantDetailsService.class)
    @DependsOnDatabaseInitialization
    JdbcTenantDetailsService tenantDetailsService(@TenantDetailsDataSource ObjectProvider<DataSource> tenantDetailsDataSource,
            ObjectProvider<DataSource> dataSource, ObjectProvider<JdbcClient> jdbcClient) {
        var dedicatedDataSource = tenantDetailsDataSource.getIfAvailable();
        if (dedicatedDataSource != null) {
            return JdbcTenantDetailsService.builder().dataSource(dedicatedDataSource).build();
        }
        return JdbcTenantDetailsService.builder()
            .jdbcClient(jdbcClient.getIfUnique(() -> JdbcClient.create(dataSource.getObject())))
            .build();
    }

    @Configuration(proxyBeanMethods = false)
    @Import(DatabaseInitializationDependencyConfigurer.class)
    static class DataSourceInitializerConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @Conditional(OnJdbcTenantDetailsDatabaseInitializationCondition.class)
        JdbcTenantDetailsDataSourceScriptDatabaseInitializer jdbcTenantDetailsDataSourceScriptDatabaseInitializer(
                @TenantDetailsDataSource ObjectProvider<DataSource> tenantDetailsDataSource,
                ObjectProvider<DataSource> dataSource, JdbcTenantDetailsProperties properties) {
            return new JdbcTenantDetailsDataSourceScriptDatabaseInitializer(
                    tenantDetailsDataSource.getIfAvailable(dataSource::getObject), properties);
        }

        static class OnJdbcTenantDetailsDatabaseInitializationCondition extends OnDatabaseInitializationCondition {

            OnJdbcTenantDetailsDatabaseInitializationCondition() {
                super("Tenant Details JDBC", JdbcTenantDetailsProperties.CONFIG_PREFIX + ".initialize-schema");
            }

        }

    }

    /**
     * Matches when a data source is qualified with {@link TenantDetailsDataSource}, or
     * when the application has a single data source candidate.
     */
    static final class OnTenantDetailsDataSourceCondition extends AnyNestedCondition {

        OnTenantDetailsDataSourceCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnBean(annotation = TenantDetailsDataSource.class)
        static final class QualifiedDataSource {}

        @ConditionalOnSingleCandidate(DataSource.class)
        static final class SingleDataSource {}

    }

}
