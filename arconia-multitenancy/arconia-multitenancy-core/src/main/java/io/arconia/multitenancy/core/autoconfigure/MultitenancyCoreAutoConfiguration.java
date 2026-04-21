package io.arconia.multitenancy.core.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import io.arconia.multitenancy.core.cache.DefaultTenantKeyGenerator;
import io.arconia.multitenancy.core.cache.TenantKeyGenerator;
import io.arconia.multitenancy.core.context.resolvers.FixedTenantResolver;

/**
 * Auto-configuration for core multitenancy.
 */
@AutoConfiguration
@EnableConfigurationProperties(FixedTenantResolutionProperties.class)
@Import({ TenantDetailsConfiguration.class, TenantObservabilityConfiguration.class })
public final class MultitenancyCoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TenantKeyGenerator.class)
    DefaultTenantKeyGenerator tenantKeyGenerator() {
        return new DefaultTenantKeyGenerator();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBooleanProperty(prefix = FixedTenantResolutionProperties.CONFIG_PREFIX, value = "enabled")
    FixedTenantResolver fixedTenantResolver(FixedTenantResolutionProperties fixedTenantResolutionProperties) {
        return new FixedTenantResolver(fixedTenantResolutionProperties.getTenantIdentifier());
    }

}
