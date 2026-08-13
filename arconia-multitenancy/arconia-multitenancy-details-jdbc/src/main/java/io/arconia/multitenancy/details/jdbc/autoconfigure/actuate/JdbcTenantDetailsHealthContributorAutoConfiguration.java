package io.arconia.multitenancy.details.jdbc.autoconfigure.actuate;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.health.autoconfigure.contributor.CompositeHealthContributorConfiguration;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import io.arconia.multitenancy.details.jdbc.JdbcTenantDetailsService;
import io.arconia.multitenancy.details.jdbc.actuate.JdbcTenantDetailsHealthIndicator;
import io.arconia.multitenancy.details.jdbc.autoconfigure.JdbcTenantDetailsAutoConfiguration;
import io.arconia.multitenancy.details.jdbc.autoconfigure.actuate.JdbcTenantDetailsHealthContributorAutoConfiguration.JdbcTenantDetailsHealthContributorConfiguration;

/**
 * Auto-configuration for the tenant details health indicator.
 */
@AutoConfiguration(after = JdbcTenantDetailsAutoConfiguration.class)
@ConditionalOnClass({ HealthContributor.class, CompositeHealthContributorConfiguration.class,
        ConditionalOnEnabledHealthIndicator.class })
@ConditionalOnBean(JdbcTenantDetailsService.class)
@ConditionalOnSingleCandidate(DataSource.class)
@Import(JdbcTenantDetailsHealthContributorConfiguration.class)
public final class JdbcTenantDetailsHealthContributorAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnEnabledHealthIndicator("tenantdetails")
    static final class JdbcTenantDetailsHealthContributorConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "jdbcTenantDetailsHealthIndicator")
        JdbcTenantDetailsHealthIndicator jdbcTenantDetailsHealthIndicator(DataSource dataSource) {
            return new JdbcTenantDetailsHealthIndicator(JdbcClient.create(dataSource));
        }

    }

}
