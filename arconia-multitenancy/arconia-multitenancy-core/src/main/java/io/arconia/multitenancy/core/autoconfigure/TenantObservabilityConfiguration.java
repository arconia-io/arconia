package io.arconia.multitenancy.core.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.multitenancy.core.observability.MdcTenantEventListener;
import io.arconia.multitenancy.core.observability.TenantObservationFilter;

/**
 * Configuration for tenant observability: Micrometer observations and MDC logging.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ TenantObservationProperties.class, TenantLoggingProperties.class })
class TenantObservabilityConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBooleanProperty(prefix = TenantObservationProperties.CONFIG_PREFIX, value = "enabled",
            matchIfMissing = true)
    TenantObservationFilter tenantObservationFilter(TenantObservationProperties tenantObservationProperties) {
        return new TenantObservationFilter(tenantObservationProperties.getKeyName(),
                tenantObservationProperties.getCardinality());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBooleanProperty(prefix = TenantLoggingProperties.CONFIG_PREFIX, value = "mdc.enabled",
            matchIfMissing = true)
    MdcTenantEventListener mdcTenantEventListener(TenantLoggingProperties tenantLoggingProperties) {
        return new MdcTenantEventListener(tenantLoggingProperties.getMdc().getKeyName());
    }

}
