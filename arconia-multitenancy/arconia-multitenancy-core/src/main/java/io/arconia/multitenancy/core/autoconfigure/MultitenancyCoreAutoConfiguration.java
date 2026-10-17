package io.arconia.multitenancy.core.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import io.arconia.multitenancy.core.cache.DefaultTenantKeyGenerator;
import io.arconia.multitenancy.core.cache.TenantKeyGenerator;
import io.arconia.multitenancy.core.resolution.DefaultTenantIdentifierValidator;
import io.arconia.multitenancy.core.resolution.TenantIdentifierValidator;

/**
 * Auto-configuration for core multitenancy.
 */
@AutoConfiguration
@EnableConfigurationProperties({ FixedTenantResolutionProperties.class, TenantIdentifierProperties.class })
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

    @Bean
    @ConditionalOnMissingBean(TenantKeyGenerator.class)
    DefaultTenantKeyGenerator tenantKeyGenerator() {
        return new DefaultTenantKeyGenerator();
    }

}
