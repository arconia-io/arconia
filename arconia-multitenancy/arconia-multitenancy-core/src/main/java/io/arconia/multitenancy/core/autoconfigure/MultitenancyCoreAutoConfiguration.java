package io.arconia.multitenancy.core.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import io.arconia.multitenancy.core.autoconfigure.tenantdetails.TenantDetailsConfiguration;
import io.arconia.multitenancy.core.cache.DefaultTenantKeyGenerator;
import io.arconia.multitenancy.core.cache.TenantKeyGenerator;
import io.arconia.multitenancy.core.context.events.HolderTenantContextEventListener;
import io.arconia.multitenancy.core.context.events.MdcTenantContextEventListener;
import io.arconia.multitenancy.core.context.events.ObservationTenantContextEventListener;
import io.arconia.multitenancy.core.context.resolvers.FixedTenantResolver;
import io.arconia.multitenancy.core.events.DefaultTenantEventPublisher;
import io.arconia.multitenancy.core.events.TenantEventPublisher;

/**
 * Auto-configuration for core multitenancy.
 */
@AutoConfiguration
@EnableConfigurationProperties({ FixedTenantResolutionProperties.class, TenantManagementProperties.class })
@Import(TenantDetailsConfiguration.class)
public final class MultitenancyCoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TenantKeyGenerator.class)
    DefaultTenantKeyGenerator tenantKeyGenerator() {
        return new DefaultTenantKeyGenerator();
    }

    @Bean
    HolderTenantContextEventListener holderTenantContextEventListener() {
        return new HolderTenantContextEventListener();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = TenantManagementProperties.CONFIG_PREFIX, value = "mdc.enabled",
            havingValue = "true", matchIfMissing = true)
    MdcTenantContextEventListener mdcTenantContextEventListener(TenantManagementProperties tenantManagementProperties) {
        return new MdcTenantContextEventListener(tenantManagementProperties.getMdc().getKey());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = TenantManagementProperties.CONFIG_PREFIX, value = "observations.enabled",
            havingValue = "true", matchIfMissing = true)
    ObservationTenantContextEventListener observationTenantContextEventListener(
            TenantManagementProperties tenantManagementProperties) {
        return new ObservationTenantContextEventListener(tenantManagementProperties.getObservations().getKey(),
                tenantManagementProperties.getObservations().getCardinality());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = FixedTenantResolutionProperties.CONFIG_PREFIX, value = "enabled",
            havingValue = "true")
    FixedTenantResolver fixedTenantResolver(FixedTenantResolutionProperties fixedTenantResolutionProperties) {
        return new FixedTenantResolver(fixedTenantResolutionProperties.getTenantIdentifier());
    }

    @Bean
    @ConditionalOnMissingBean
    TenantEventPublisher tenantEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        return new DefaultTenantEventPublisher(applicationEventPublisher);
    }

}
