package io.arconia.multitenancy.tenantdetails.jdbc.autoconfigure;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.sql.autoconfigure.init.OnDatabaseInitializationCondition;
import org.springframework.boot.sql.init.dependency.DatabaseInitializationDependencyConfigurer;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import io.arconia.multitenancy.core.autoconfigure.MultitenancyCoreAutoConfiguration;
import io.arconia.multitenancy.core.tenantdetails.TenantDetailsService;
import io.arconia.multitenancy.tenantdetails.jdbc.JdbcTenantDetailsService;

/**
 * Auto-configuration for JDBC-based tenant details.
 */
@AutoConfiguration(after = DataSourceAutoConfiguration.class, before = MultitenancyCoreAutoConfiguration.class)
@ConditionalOnSingleCandidate(DataSource.class)
@ConditionalOnBooleanProperty(prefix = JdbcTenantDetailsProperties.CONFIG_PREFIX, value = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(JdbcTenantDetailsProperties.class)
public final class JdbcTenantDetailsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TenantDetailsService.class)
    @DependsOnDatabaseInitialization
    JdbcTenantDetailsService tenantDetailsService(DataSource dataSource) {
        return JdbcTenantDetailsService.builder().dataSource(dataSource).build();
    }

    @Configuration(proxyBeanMethods = false)
    @Import(DatabaseInitializationDependencyConfigurer.class)
    static class DataSourceInitializerConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @Conditional(OnJdbcTenantDetailsDatabaseInitializationCondition.class)
        JdbcTenantDetailsDataSourceScriptDatabaseInitializer jdbcTenantDetailsDataSourceScriptDatabaseInitializer(
                DataSource dataSource, JdbcTenantDetailsProperties properties) {
            return new JdbcTenantDetailsDataSourceScriptDatabaseInitializer(dataSource, properties);
        }

        static class OnJdbcTenantDetailsDatabaseInitializationCondition extends OnDatabaseInitializationCondition {

            OnJdbcTenantDetailsDatabaseInitializationCondition() {
                super("Tenant Details JDBC", JdbcTenantDetailsProperties.CONFIG_PREFIX + ".initialize-schema");
            }

        }

    }

}
