package io.arconia.multitenancy.core.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.multitenancy.core.cache.TenantKeyGenerator;
import io.arconia.multitenancy.core.observability.MdcTenantEventListener;
import io.arconia.multitenancy.core.observability.TenantObservationFilter;
import io.arconia.multitenancy.core.resolution.DefaultTenantIdentifierValidator;
import io.arconia.multitenancy.core.resolution.InvalidTenantIdentifierException;
import io.arconia.multitenancy.core.resolution.TenantIdentifierValidator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link MultitenancyCoreAutoConfiguration}.
 */
class MultitenancyCoreAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(MultitenancyCoreAutoConfiguration.class));

    @Test
    void tenantKeyGenerator() {
        contextRunner.run(context ->
            assertThat(context).hasSingleBean(TenantKeyGenerator.class)
        );
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
        contextRunner.run(context ->
            assertThat(context).hasSingleBean(TenantObservationFilter.class)
        );
    }

    @Test
    void tenantObservationFilterWhenDisabled() {
        contextRunner.withPropertyValues("arconia.multitenancy.observations.enabled=false").run(context ->
            assertThat(context).doesNotHaveBean(TenantObservationFilter.class)
        );
    }

    @Test
    void mdcTenantContextEventListenerWhenDefault() {
        contextRunner.run(context ->
            assertThat(context).hasSingleBean(MdcTenantEventListener.class)
        );
    }

    @Test
    void mdcTenantContextEventListenerWhenDisabled() {
        contextRunner.withPropertyValues("arconia.multitenancy.logging.mdc.enabled=false").run(context ->
            assertThat(context).doesNotHaveBean(MdcTenantEventListener.class)
        );
    }

    @Test
    void tenantIdentifierValidatorWhenDefaultPolicy() {
        contextRunner.run(context -> {
            var validator = context.getBean(DefaultTenantIdentifierValidator.class);
            assertThat(validator.getMaxLength()).isEqualTo(64);
            assertThat(validator.getPattern()).isEqualTo("[a-zA-Z0-9_-]+");
        });
    }

    @Test
    void tenantIdentifierValidatorWhenCustomPolicy() {
        contextRunner
            .withPropertyValues("arconia.multitenancy.identifier.max-length=8",
                    "arconia.multitenancy.identifier.pattern=[a-z]+")
            .run(context -> {
                var validator = context.getBean(DefaultTenantIdentifierValidator.class);
                assertThat(validator.getMaxLength()).isEqualTo(8);
                assertThat(validator.getPattern()).isEqualTo("[a-z]+");
                assertThatThrownBy(() -> validator.validate("ACME")).isInstanceOf(InvalidTenantIdentifierException.class);
            });
    }

    @Test
    void tenantIdentifierValidatorWhenMalformedPatternThenFailFast() {
        contextRunner.withPropertyValues("arconia.multitenancy.identifier.pattern=[a-z").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining("pattern is not a valid regular expression")
                .rootCause()
                .isInstanceOf(java.util.regex.PatternSyntaxException.class);
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
