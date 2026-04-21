package io.arconia.multitenancy.core.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.arconia.multitenancy.core.tenantdetails.TenantDetailsService;
import io.arconia.multitenancy.core.tenantdetails.TenantVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TenantDetailsConfiguration}.
 */
class TenantDetailsConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(TenantDetailsConfiguration.class));

    @Test
    void tenantDetailsServiceWhenDefault() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(TenantDetailsService.class);
        });
    }

    @Test
    void tenantDetailsServiceWhenProperties() {
        contextRunner.withPropertyValues("arconia.multitenancy.details.source=properties").run(context -> {
            assertThat(context).hasSingleBean(TenantDetailsService.class);
        });
    }

    @Test
    void tenantVerifierWhenNoService() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(TenantVerifier.class);
        });
    }

    @Test
    void tenantVerifierWhenService() {
        contextRunner.withPropertyValues("arconia.multitenancy.details.source=properties").run(context -> {
            assertThat(context).hasSingleBean(TenantVerifier.class);
        });
    }

}
