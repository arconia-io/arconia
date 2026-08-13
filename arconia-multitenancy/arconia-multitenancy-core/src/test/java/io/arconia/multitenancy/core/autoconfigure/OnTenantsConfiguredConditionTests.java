package io.arconia.multitenancy.core.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OnTenantsConfiguredCondition}.
 */
class OnTenantsConfiguredConditionTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(ConditionalConfiguration.class);

    @Test
    void whenNoTenantsConfiguredThenNoMatch() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean("conditionalBean");
        });
    }

    @Test
    void whenSingleTenantConfiguredThenMatch() {
        contextRunner.withPropertyValues("arconia.multitenancy.details.tenants[0].identifier=acme").run(context -> {
            assertThat(context).hasBean("conditionalBean");
        });
    }

    @Test
    void whenMultipleTenantsConfiguredThenMatch() {
        contextRunner
            .withPropertyValues("arconia.multitenancy.details.tenants[0].identifier=acme",
                    "arconia.multitenancy.details.tenants[1].identifier=beans")
            .run(context -> {
                assertThat(context).hasBean("conditionalBean");
            });
    }

    @Test
    void whenTenantConfiguredButDisabledThenMatch() {
        contextRunner
            .withPropertyValues("arconia.multitenancy.details.tenants[0].identifier=acme",
                    "arconia.multitenancy.details.tenants[0].enabled=false")
            .run(context -> {
                assertThat(context).hasBean("conditionalBean");
            });
    }

    @Test
    void whenTenantIdentifierIsEmptyThenFailFast() {
        contextRunner.withPropertyValues("arconia.multitenancy.details.tenants[0].identifier=").run(context -> {
            assertThat(context).hasFailed();
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class ConditionalConfiguration {

        @Bean
        @Conditional(OnTenantsConfiguredCondition.class)
        String conditionalBean() {
            return "conditional";
        }

    }

}
