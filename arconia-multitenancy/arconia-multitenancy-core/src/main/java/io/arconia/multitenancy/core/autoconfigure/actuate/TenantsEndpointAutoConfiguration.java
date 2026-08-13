package io.arconia.multitenancy.core.autoconfigure.actuate;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import io.arconia.multitenancy.core.actuate.endpoint.TenantsEndpoint;
import io.arconia.multitenancy.core.autoconfigure.MultitenancyCoreAutoConfiguration;
import io.arconia.multitenancy.core.tenantdetails.TenantDetailsService;

/**
 * Auto-configuration for the tenants Actuator endpoint.
 */
@AutoConfiguration(after = MultitenancyCoreAutoConfiguration.class)
@ConditionalOnClass(Endpoint.class)
@ConditionalOnBean(TenantDetailsService.class)
public final class TenantsEndpointAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    TenantsEndpoint tenantsEndpoint(TenantDetailsService tenantDetailsService) {
        return new TenantsEndpoint(tenantDetailsService);
    }

}
