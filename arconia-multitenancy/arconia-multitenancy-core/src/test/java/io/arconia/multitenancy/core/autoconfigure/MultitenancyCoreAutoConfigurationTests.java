package io.arconia.multitenancy.core.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.multitenancy.core.cache.TenantKeyGenerator;
import io.arconia.multitenancy.core.context.resolvers.FixedTenantResolver;
import io.arconia.multitenancy.core.observability.MdcTenantEventListener;
import io.arconia.multitenancy.core.observability.TenantObservationFilter;
import io.arconia.multitenancy.core.tenantdetails.DefaultTenantIdentifierValidator;
import io.arconia.multitenancy.core.tenantdetails.TenantIdentifierValidator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MultitenancyCoreAutoConfiguration}.
 */
class MultitenancyCoreAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(MultitenancyCoreAutoConfiguration.class));

    @Test
    void tenantKeyGenerator() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(TenantKeyGenerator.class);
        });
    }

    @Test
    void tenantIdentifierValidatorWhenNoTenantDetailsService() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(TenantIdentifierValidator.class);
            assertThat(context).hasSingleBean(DefaultTenantIdentifierValidator.class);
        });
    }

    @Test
    void tenantIdentifierValidatorWhenCustom() {
        contextRunner.withUserConfiguration(CustomTenantIdentifierValidatorConfiguration.class).run(context -> {
            assertThat(context).hasSingleBean(TenantIdentifierValidator.class);
            assertThat(context).doesNotHaveBean(DefaultTenantIdentifierValidator.class);
        });
    }

    @Test
    void tenantObservationFilterWhenDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(TenantObservationFilter.class);
        });
    }

    @Test
    void tenantObservationFilterWhenDisabled() {
        contextRunner.withPropertyValues("arconia.multitenancy.observations.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(TenantObservationFilter.class);
        });
    }

    @Test
    void mdcTenantContextEventListenerWhenDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(MdcTenantEventListener.class);
        });
    }

    @Test
    void mdcTenantContextEventListenerWhenDisabled() {
        contextRunner.withPropertyValues("arconia.multitenancy.logging.mdc.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(MdcTenantEventListener.class);
        });
    }

    @Test
    void fixedTenantResolverWhenDefault() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(FixedTenantResolver.class);
        });
    }

    @Test
    void fixedTenantResolverWhenEnabled() {
        contextRunner.withPropertyValues("arconia.multitenancy.resolution.fixed.enabled=true").run(context -> {
            assertThat(context).hasSingleBean(FixedTenantResolver.class);
        });
    }

    @Test
    void fixedTenantResolverWhenEnabledUsesDefaultIdentifier() {
        contextRunner.withPropertyValues("arconia.multitenancy.resolution.fixed.enabled=true").run(context -> {
            assertThat(context.getBean(FixedTenantResolver.class).resolveTenantIdentifier(this)).isEqualTo("default");
        });
    }

    @Test
    void fixedTenantResolverWhenCustomIdentifier() {
        contextRunner
            .withPropertyValues("arconia.multitenancy.resolution.fixed.enabled=true",
                    "arconia.multitenancy.resolution.fixed.tenant-identifier=acme")
            .run(context -> {
                assertThat(context.getBean(FixedTenantResolver.class).resolveTenantIdentifier(this)).isEqualTo("acme");
            });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomTenantIdentifierValidatorConfiguration {

        @Bean
        TenantIdentifierValidator customTenantIdentifierValidator() {
            return tenantIdentifier -> {};
        }

    }

}
