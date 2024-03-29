package io.arconia.autoconfigure.multitenancy.core;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

import io.arconia.autoconfigure.multitenancy.core.tenantdetails.PropertiesTenantDetailsService;
import io.arconia.autoconfigure.multitenancy.core.tenantdetails.TenantDetailsProperties;
import io.arconia.core.multitenancy.cache.DefaultTenantKeyGenerator;
import io.arconia.core.multitenancy.cache.TenantKeyGenerator;
import io.arconia.core.multitenancy.context.events.HolderTenantContextEventListener;
import io.arconia.core.multitenancy.context.events.MdcTenantContextEventListener;
import io.arconia.core.multitenancy.context.events.ObservationTenantContextEventListener;
import io.arconia.core.multitenancy.context.resolvers.FixedTenantResolver;
import io.arconia.core.multitenancy.events.DefaultTenantEventPublisher;
import io.arconia.core.multitenancy.events.TenantEventPublisher;
import io.arconia.core.multitenancy.tenantdetails.TenantDetailsService;

/**
 * Auto-configuration for core multitenancy.
 */
@AutoConfiguration
@EnableConfigurationProperties({ FixedTenantResolutionProperties.class, TenantDetailsProperties.class,
        TenantManagementProperties.class })
public class MultitenancyCoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    TenantKeyGenerator tenantKeyGenerator() {
        return new DefaultTenantKeyGenerator();
    }

    @Bean
    @ConditionalOnMissingBean
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

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = TenantDetailsProperties.CONFIG_PREFIX, name = "source", havingValue = "properties",
            matchIfMissing = true)
    TenantDetailsService tenantDetailsService(TenantDetailsProperties tenantDetailsProperties) {
        return new PropertiesTenantDetailsService(tenantDetailsProperties);
    }

}
