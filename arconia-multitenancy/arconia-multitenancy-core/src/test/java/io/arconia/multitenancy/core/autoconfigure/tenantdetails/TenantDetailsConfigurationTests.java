package io.arconia.multitenancy.core.autoconfigure.tenantdetails;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.arconia.multitenancy.core.context.events.ValidatingTenantContextEventListener;
import io.arconia.multitenancy.core.tenantdetails.TenantDetailsService;

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
    void validatingTenantContextEventListenerWhenNoService() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(ValidatingTenantContextEventListener.class);
        });
    }

    @Test
    void validatingTenantContextEventListenerWhenService() {
        contextRunner.withPropertyValues("arconia.multitenancy.details.source=properties").run(context -> {
            assertThat(context).hasSingleBean(ValidatingTenantContextEventListener.class);
        });
    }

}
