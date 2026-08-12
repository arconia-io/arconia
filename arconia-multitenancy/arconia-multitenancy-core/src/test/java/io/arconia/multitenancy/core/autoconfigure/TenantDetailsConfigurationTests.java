package io.arconia.multitenancy.core.autoconfigure;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.multitenancy.core.tenantdetails.TenantDetails;
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
    void tenantDetailsServiceWhenTenantsConfigured() {
        contextRunner.withPropertyValues("arconia.multitenancy.details.tenants[0].identifier=acme").run(context -> {
            assertThat(context).hasSingleBean(TenantDetailsService.class);
            assertThat(context).hasSingleBean(PropertiesTenantDetailsService.class);
        });
    }

    @Test
    void tenantDetailsServiceWhenCustom() {
        contextRunner.withPropertyValues("arconia.multitenancy.details.tenants[0].identifier=acme")
            .withUserConfiguration(CustomTenantDetailsServiceConfiguration.class)
            .run(context -> {
                assertThat(context).hasSingleBean(TenantDetailsService.class);
                assertThat(context).doesNotHaveBean(PropertiesTenantDetailsService.class);
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
        contextRunner.withPropertyValues("arconia.multitenancy.details.tenants[0].identifier=acme").run(context -> {
            assertThat(context).hasSingleBean(TenantVerifier.class);
        });
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

}
