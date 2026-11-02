package io.arconia.multitenancy.core.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import io.arconia.multitenancy.core.cache.TenantKeyGenerator;
import io.arconia.multitenancy.core.context.TenantContextPropagatingTaskDecorator;
import io.arconia.multitenancy.core.resolution.DefaultTenantIdentifierValidator;
import io.arconia.multitenancy.core.resolution.TenantIdentifierValidator;

/**
 * Auto-configuration for core multitenancy.
 */
@AutoConfiguration
@EnableConfigurationProperties({ FixedTenantResolutionProperties.class, TenantContextProperties.class,
        TenantIdentifierProperties.class })
@Import({ TenantDetailsConfiguration.class, TenantObservabilityConfiguration.class })
public final class MultitenancyCoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TenantIdentifierValidator.class)
    DefaultTenantIdentifierValidator tenantIdentifierValidator(TenantIdentifierProperties tenantIdentifierProperties) {
        return DefaultTenantIdentifierValidator.builder()
            .maxLength(tenantIdentifierProperties.getMaxLength())
            .pattern(tenantIdentifierProperties.getPattern())
            .build();
    }

    /**
     * Spring Boot applies every {@code TaskDecorator} bean to the executors and
     * schedulers it auto-configures, so this reaches {@code @Async} methods and
     * {@code Callable} controller methods with no further wiring.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBooleanProperty(prefix = TenantContextProperties.CONFIG_PREFIX, value = "propagation.enabled", matchIfMissing = true)
    TenantContextPropagatingTaskDecorator tenantContextPropagatingTaskDecorator(TenantLoggingProperties tenantLoggingProperties) {
        var mdc = tenantLoggingProperties.getMdc();
        return TenantContextPropagatingTaskDecorator.builder()
            .mdcKeyName(mdc.isEnabled() ? mdc.getKeyName() : null)
            .build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "tenantKeyGenerator")
    TenantKeyGenerator tenantKeyGenerator() {
        return new TenantKeyGenerator();
    }

}
