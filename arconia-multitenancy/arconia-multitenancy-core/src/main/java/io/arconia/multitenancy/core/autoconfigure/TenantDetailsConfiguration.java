package io.arconia.multitenancy.core.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import io.arconia.multitenancy.core.resolution.DefaultTenantVerifier;
import io.arconia.multitenancy.core.resolution.TenantVerifier;
import io.arconia.multitenancy.core.tenantdetails.InMemoryTenantDetailsService;
import io.arconia.multitenancy.core.tenantdetails.Tenant;
import io.arconia.multitenancy.core.tenantdetails.TenantDetailsService;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TenantDetailsProperties.class)
final class TenantDetailsConfiguration {

    @Bean
    @ConditionalOnMissingBean(TenantDetailsService.class)
    @Conditional(OnTenantsConfiguredCondition.class)
    InMemoryTenantDetailsService tenantDetailsService(TenantDetailsProperties tenantDetailsProperties) {
        var tenants = tenantDetailsProperties.getTenants()
            .stream()
            .map(tenantConfig -> Tenant.builder()
                .identifier(tenantConfig.getIdentifier())
                .enabled(tenantConfig.isEnabled())
                .attributes(tenantConfig.getAttributes())
                .build())
            .toList();
        return new InMemoryTenantDetailsService(tenants);
    }

    @Bean
    @ConditionalOnMissingBean(TenantVerifier.class)
    @ConditionalOnBean(TenantDetailsService.class)
    DefaultTenantVerifier tenantVerifier(TenantDetailsService tenantDetailsService) {
        return new DefaultTenantVerifier(tenantDetailsService);
    }

}
