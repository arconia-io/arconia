package io.arconia.autoconfigure.core.multitenancy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.arconia.core.multitenancy.cache.TenantKeyGenerator;
import io.arconia.core.multitenancy.context.events.HolderTenantContextEventListener;
import io.arconia.core.multitenancy.context.events.MdcTenantContextEventListener;
import io.arconia.core.multitenancy.context.events.ObservationTenantContextEventListener;
import io.arconia.core.multitenancy.context.resolvers.FixedTenantResolver;
import io.arconia.core.multitenancy.events.TenantEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MultitenancyCoreAutoConfiguration}.
 *
 * @author Thomas Vitale
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
    void holderTenantContextEventListener() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(HolderTenantContextEventListener.class);
        });
    }

    @Test
    void observationTenantContextEventListenerWhenDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ObservationTenantContextEventListener.class);
        });
    }

    @Test
    void observationTenantContextEventListenerWhenDisabled() {
        contextRunner.withPropertyValues("arconia.multitenancy.management.observations.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(ObservationTenantContextEventListener.class);
        });
    }

    @Test
    void mdcTenantContextEventListenerWhenDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(MdcTenantContextEventListener.class);
        });
    }

    @Test
    void mdcTenantContextEventListenerWhenDisabled() {
        contextRunner.withPropertyValues("arconia.multitenancy.management.mdc.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(MdcTenantContextEventListener.class);
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
    void tenantEventPublisher() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(TenantEventPublisher.class);
        });
    }

}
