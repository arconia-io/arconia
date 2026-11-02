package io.arconia.multitenancy.core.autoconfigure;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.benmanes.caffeine.cache.Caffeine;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.multitenancy.core.resolution.InvalidTenantIdentifierException;
import io.arconia.multitenancy.core.resolution.TenantIdentifierValidator;
import io.arconia.multitenancy.core.resolution.TenantVerifier;
import io.arconia.multitenancy.core.tenantdetails.InMemoryTenantDetailsService;
import io.arconia.multitenancy.core.tenantdetails.Tenant;
import io.arconia.multitenancy.core.tenantdetails.TenantDetails;
import io.arconia.multitenancy.core.tenantdetails.TenantDetailsService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TenantDetailsConfiguration}.
 */
class TenantDetailsConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(MultitenancyCoreAutoConfiguration.class));

    @Test
    void tenantDetailsServiceWhenDefault() {
        contextRunner.run(context ->
            assertThat(context).doesNotHaveBean(TenantDetailsService.class)
        );
    }

    @Test
    void tenantDetailsServiceWhenTenantsConfigured() {
        contextRunner.withPropertyValues("arconia.multitenancy.details.tenants[0].identifier=acme").run(context -> {
            assertThat(context).hasSingleBean(TenantDetailsService.class);
            assertThat(context).hasSingleBean(InMemoryTenantDetailsService.class);
        });
    }

    @Test
    void tenantDetailsServiceWhenTenantsConfiguredThenTenantsAreBound() {
        contextRunner
            .withPropertyValues("arconia.multitenancy.details.tenants[0].identifier=acme",
                    "arconia.multitenancy.details.tenants[1].identifier=beans",
                    "arconia.multitenancy.details.tenants[1].enabled=false")
            .run(context -> {
                var tenantDetailsService = context.getBean(TenantDetailsService.class);

                assertThat(tenantDetailsService.loadAllTenants()).hasSize(2);
                assertThat(tenantDetailsService.loadTenantByIdentifier("beans")).isNotNull()
                    .extracting(tenantDetails -> tenantDetails != null && tenantDetails.enabled())
                    .isEqualTo(false);
            });
    }

    @Test
    void tenantDetailsServiceWhenCustom() {
        contextRunner.withPropertyValues("arconia.multitenancy.details.tenants[0].identifier=acme")
            .withUserConfiguration(CustomTenantDetailsServiceConfiguration.class)
            .run(context -> {
                assertThat(context).hasSingleBean(TenantDetailsService.class);
                assertThat(context).doesNotHaveBean(InMemoryTenantDetailsService.class);
            });
    }

    @Test
    void tenantVerifierWhenNoService() {
        contextRunner.run(context ->
            assertThat(context).doesNotHaveBean(TenantVerifier.class)
        );
    }

    @Test
    void tenantVerifierWhenService() {
        contextRunner.withPropertyValues("arconia.multitenancy.details.tenants[0].identifier=acme").run(context -> {
            assertThat(context).hasSingleBean(TenantVerifier.class);
        });
    }

    @Test
    void tenantVerifierWhenCacheTimeToLiveThenLookupsAreCached() {
        contextRunner.withUserConfiguration(CountingTenantDetailsServiceConfiguration.class)
            .withPropertyValues("arconia.multitenancy.details.cache.time-to-live=1m")
            .run(context -> {
                var verifier = context.getBean(TenantVerifier.class);
                var service = context.getBean(CountingTenantDetailsService.class);

                verifier.verify("acme");
                verifier.verify("acme");

                assertThat(service.lookups).hasValue(1);
            });
    }

    @Test
    void tenantVerifierWhenNoCacheTimeToLiveThenLookupsAreNotCached() {
        contextRunner.withUserConfiguration(CountingTenantDetailsServiceConfiguration.class).run(context -> {
            var verifier = context.getBean(TenantVerifier.class);
            var service = context.getBean(CountingTenantDetailsService.class);

            verifier.verify("acme");
            verifier.verify("acme");

            assertThat(service.lookups).hasValue(2);
        });
    }

    @Test
    void tenantVerifierWhenCacheTimeToLiveAndNoCaffeineThenFail() {
        contextRunner.withUserConfiguration(CountingTenantDetailsServiceConfiguration.class)
            .withClassLoader(new FilteredClassLoader(Caffeine.class))
            .withPropertyValues("arconia.multitenancy.details.cache.time-to-live=1m")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context).getFailure()
                    .rootCause()
                    .hasMessageContaining("'arconia.multitenancy.details.cache.time-to-live' property is set, but Caffeine is not on the classpath");
            });
    }

    @Test
    void tenantsEndpointServiceIsNotCached() {
        contextRunner.withUserConfiguration(CountingTenantDetailsServiceConfiguration.class)
            .withPropertyValues("arconia.multitenancy.details.cache.time-to-live=1m")
            .run(context -> assertThat(context.getBean(TenantDetailsService.class))
                .isInstanceOf(CountingTenantDetailsService.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class CountingTenantDetailsServiceConfiguration {

        @Bean
        CountingTenantDetailsService countingTenantDetailsService() {
            return new CountingTenantDetailsService();
        }

    }

    static class CountingTenantDetailsService implements TenantDetailsService {

        final AtomicInteger lookups = new AtomicInteger();

        @Override
        public List<? extends TenantDetails> loadAllTenants() {
            return List.of();
        }

        @Override
        public TenantDetails loadTenantByIdentifier(String identifier) {
            lookups.incrementAndGet();
            return Tenant.builder().identifier(identifier).build();
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

    @Test
    void whenDuplicateTenantIdentifiersThenFail() {
        contextRunner
            .withPropertyValues("arconia.multitenancy.details.tenants[0].identifier=acme",
                    "arconia.multitenancy.details.tenants[1].identifier=acme")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure()).rootCause()
                    .hasMessageContaining("found duplicates: acme");
            });
    }

    @Test
    void whenTenantWithEmptyIdentifierThenFail() {
        contextRunner.withPropertyValues("arconia.multitenancy.details.tenants[0].identifier=").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause()
                .hasMessage("The 'arconia.multitenancy.details.tenants[0].identifier' property must be set");
        });
    }

    @Test
    void whenTenantWithoutIdentifierThenFail() {
        contextRunner
            .withPropertyValues("arconia.multitenancy.details.tenants[0].identifier=acme",
                    "arconia.multitenancy.details.tenants[1].enabled=false")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure()).rootCause()
                    .hasMessage("The 'arconia.multitenancy.details.tenants[1].identifier' property must be set");
            });
    }

    @Test
    void whenTenantIdentifierHasInvalidCharactersThenFail() {
        contextRunner
            .withPropertyValues("arconia.multitenancy.details.tenants[0].identifier=acme",
                    "arconia.multitenancy.details.tenants[1].identifier=acme corp")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasStackTraceContaining("The 'arconia.multitenancy.details.tenants[1].identifier' property is not a valid tenant identifier");
            });
    }

    @Test
    void whenTenantIdentifierTooLongThenFail() {
        contextRunner
            .withPropertyValues("arconia.multitenancy.identifier.max-length=4",
                    "arconia.multitenancy.details.tenants[0].identifier=acme-corp")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasStackTraceContaining("The 'arconia.multitenancy.details.tenants[0].identifier' property is not a valid tenant identifier");
            });
    }

    @Test
    void whenTenantIdentifierMatchesCustomPatternThenBound() {
        contextRunner
            .withPropertyValues("arconia.multitenancy.identifier.pattern=[a-z.]+",
                    "arconia.multitenancy.details.tenants[0].identifier=acme.corp")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBean(TenantDetailsService.class).loadTenantByIdentifier("acme.corp")).isNotNull();
            });
    }

    @Test
    void whenCustomTenantIdentifierValidatorThenUsed() {
        contextRunner
            .withPropertyValues("arconia.multitenancy.details.tenants[0].identifier=acme")
            .withBean(TenantIdentifierValidator.class, () -> tenantIdentifier -> {
                throw new InvalidTenantIdentifierException("rejected by the custom validator");
            })
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasStackTraceContaining("is not a valid tenant identifier: rejected by the custom validator");
            });
    }

}
