package io.arconia.multitenancy.tenantdetails.jdbc.autoconfigure;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.sql.autoconfigure.init.OnDatabaseInitializationCondition;
import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import static io.arconia.multitenancy.tenantdetails.jdbc.autoconfigure.JdbcTenantDetailsProperties.CONFIG_PREFIX;

@AutoConfiguration
@EnableConfigurationProperties(JdbcTenantDetailsProperties.class)
public final class JdbcTenantDetailsAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @Conditional(OnArconiaTenantDetailsJdbcDatasourceInitializationCondition.class)
    static class DataSourceInitializerConfiguration {

        @Bean
        @ConditionalOnMissingBean
        JdbcTenantDetailsDataSourceScriptDatabaseInitializer arconiaDataJdbcSourceInitializer(
                DataSource dataSource,
                JdbcTenantDetailsProperties properties) {
            return new JdbcTenantDetailsDataSourceScriptDatabaseInitializer(dataSource, properties);
        }

    }

    static class OnArconiaTenantDetailsJdbcDatasourceInitializationCondition extends OnDatabaseInitializationCondition {

        OnArconiaTenantDetailsJdbcDatasourceInitializationCondition() {
            super("Arconia TenantDetails JDBC", CONFIG_PREFIX + ".initialize-schema");
        }

    }

}

