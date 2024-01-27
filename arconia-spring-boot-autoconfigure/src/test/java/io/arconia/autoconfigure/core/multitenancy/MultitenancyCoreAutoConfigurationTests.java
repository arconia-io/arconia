package io.arconia.autoconfigure.core.multitenancy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.arconia.core.multitenancy.cache.TenantKeyGenerator;
import io.arconia.core.multitenancy.context.events.HolderTenantContextEventListener;
import io.arconia.core.multitenancy.context.events.MdcTenantContextEventListener;
import io.arconia.core.multitenancy.context.events.ObservationTenantContextEventListener;
import io.arconia.core.multitenancy.context.resolvers.FixedTenantResolver;
import io.arconia.core.multitenancy.events.DefaultTenantEventPublisher;
import io.arconia.core.multitenancy.events.TenantEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
            var bean = context.getBean(TenantKeyGenerator.class);
            assertThat(bean).isInstanceOf(TenantKeyGenerator.class);
        });
    }

    @Test
    void holderTenantContextEventListener() {
        contextRunner.run(context -> {
            var bean = context.getBean(HolderTenantContextEventListener.class);
            assertThat(bean).isInstanceOf(HolderTenantContextEventListener.class);
        });
    }

    @Test
    void observationTenantContextEventListenerWhenDefault() {
        contextRunner.run(context -> {
            var bean = context.getBean(ObservationTenantContextEventListener.class);
            assertThat(bean).isInstanceOf(ObservationTenantContextEventListener.class);
        });
    }

    @Test
    void observationTenantContextEventListenerWhenDisabled() {
        contextRunner.withPropertyValues("arconia.multitenancy.management.observations.enabled=false").run(context -> {
            assertThatThrownBy(() -> context.getBean(ObservationTenantContextEventListener.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
        });
    }

    @Test
    void mdcTenantContextEventListenerWhenDefault() {
        contextRunner.run(context -> {
            var bean = context.getBean(MdcTenantContextEventListener.class);
            assertThat(bean).isInstanceOf(MdcTenantContextEventListener.class);
        });
    }

    @Test
    void mdcTenantContextEventListenerWhenDisabled() {
        contextRunner.withPropertyValues("arconia.multitenancy.management.mdc.enabled=false").run(context -> {
            assertThatThrownBy(() -> context.getBean(MdcTenantContextEventListener.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
        });
    }

    @Test
    void fixedTenantResolverWhenDefault() {
        contextRunner.run(context -> {
            assertThatThrownBy(() -> context.getBean(FixedTenantResolver.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
        });
    }

    @Test
    void fixedTenantResolverWhenEnabled() {
        contextRunner.withPropertyValues("arconia.multitenancy.resolution.fixed.enabled=true").run(context -> {
            var bean = context.getBean(FixedTenantResolver.class);
            assertThat(bean).isInstanceOf(FixedTenantResolver.class);
        });
    }

    @Test
    void tenantEventPublisher() {
        contextRunner.run(context -> {
            var bean = context.getBean(TenantEventPublisher.class);
            assertThat(bean).isInstanceOf(DefaultTenantEventPublisher.class);
        });
    }

}
