package io.arconia.multitenancy.data.autoconfigure;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.sql.autoconfigure.init.OnDatabaseInitializationCondition;
import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import static io.arconia.multitenancy.data.autoconfigure.MultitenancyDataJdbcProperties.CONFIG_PREFIX;

@AutoConfiguration
@EnableConfigurationProperties(MultitenancyDataJdbcProperties.class)
public class MultitenancyDataJdbcAutoConfiguration {


    @Configuration(proxyBeanMethods = false)
    @Conditional(OnArconiaDataJdbcDatasourceInitializationCondition.class)
    static class DataSourceInitializerConfiguration {

        @Bean
        @ConditionalOnMissingBean
        DataJdbcDataSourceScriptDatabaseInitializer arconiaDataJdbcSourceInitializer(
                DataSource dataSource,
                MultitenancyDataJdbcProperties properties) {
            return new DataJdbcDataSourceScriptDatabaseInitializer( dataSource,
                    properties);
        }

    }


    static class OnArconiaDataJdbcDatasourceInitializationCondition extends OnDatabaseInitializationCondition {

        OnArconiaDataJdbcDatasourceInitializationCondition() {
            super("Arconia Data JDBC", CONFIG_PREFIX +
                    ".initialize-schema");
        }

    }
}

