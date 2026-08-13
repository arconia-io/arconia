package io.arconia.multitenancy.details.jdbc.autoconfigure.actuate;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.arconia.multitenancy.core.autoconfigure.MultitenancyCoreAutoConfiguration;
import io.arconia.multitenancy.details.jdbc.actuate.JdbcTenantDetailsHealthIndicator;
import io.arconia.multitenancy.details.jdbc.autoconfigure.JdbcTenantDetailsAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JdbcTenantDetailsHealthContributorAutoConfiguration}.
 */
class JdbcTenantDetailsHealthContributorAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
                MultitenancyCoreAutoConfiguration.class, JdbcTenantDetailsAutoConfiguration.class,
                JdbcTenantDetailsHealthContributorAutoConfiguration.class));

    @Test
    void healthIndicatorWhenDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(JdbcTenantDetailsHealthIndicator.class);
        });
    }

    @Test
    void healthIndicatorWhenDisabled() {
        contextRunner.withPropertyValues("management.health.tenantdetails.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(JdbcTenantDetailsHealthIndicator.class);
        });
    }

    @Test
    void healthIndicatorWhenTenantDetailsServiceDisabled() {
        contextRunner.withPropertyValues("arconia.multitenancy.details.jdbc.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(JdbcTenantDetailsHealthIndicator.class);
        });
    }

    @Test
    void healthIndicatorWhenHealthNotOnClasspath() {
        contextRunner.withClassLoader(new FilteredClassLoader(HealthContributor.class)).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(JdbcTenantDetailsHealthIndicator.class);
        });
    }

}
