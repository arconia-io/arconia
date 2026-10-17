package io.arconia.multitenancy.core.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

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
        var tenants = tenantDetailsProperties.getTenants().stream()
                .map(TenantDetailsConfiguration::toTenant)
                .toList();
        return new InMemoryTenantDetailsService(tenants);
    }

    private static Tenant toTenant(TenantDetailsProperties.TenantConfig tenantConfig) {
        var identifier = tenantConfig.getIdentifier();
        Assert.notNull(identifier,
                "Each tenant in '" + TenantDetailsProperties.CONFIG_PREFIX + ".tenants' must have an identifier");
        return Tenant.builder()
            .identifier(identifier)
            .enabled(tenantConfig.isEnabled())
            .attributes(tenantConfig.getAttributes())
            .build();
    }

    @Bean
    @ConditionalOnMissingBean(TenantVerifier.class)
    @ConditionalOnBean(TenantDetailsService.class)
    DefaultTenantVerifier tenantVerifier(TenantDetailsService tenantDetailsService) {
        return new DefaultTenantVerifier(tenantDetailsService);
    }

}
