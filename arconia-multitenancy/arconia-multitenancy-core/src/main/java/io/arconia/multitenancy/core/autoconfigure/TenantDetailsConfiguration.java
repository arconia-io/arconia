package io.arconia.multitenancy.core.autoconfigure;

import java.util.ArrayList;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import io.arconia.multitenancy.core.resolution.DefaultTenantVerifier;
import io.arconia.multitenancy.core.resolution.InvalidTenantIdentifierException;
import io.arconia.multitenancy.core.resolution.TenantIdentifierValidator;
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
    InMemoryTenantDetailsService tenantDetailsService(TenantDetailsProperties tenantDetailsProperties, TenantIdentifierValidator tenantIdentifierValidator) {
        var tenantConfigs = tenantDetailsProperties.getTenants();
        var tenants = new ArrayList<Tenant>(tenantConfigs.size());
        for (int i = 0; i < tenantConfigs.size(); i++) {
            tenants.add(toTenant(tenantConfigs.get(i), i, tenantIdentifierValidator));
        }
        return new InMemoryTenantDetailsService(tenants);
    }

    private static Tenant toTenant(TenantDetailsProperties.TenantConfig tenantConfig, int index, TenantIdentifierValidator tenantIdentifierValidator) {
        var identifier = tenantConfig.getIdentifier();
        Assert.hasText(identifier, "identifier cannot be null or empty");
        try {
            tenantIdentifierValidator.validate(identifier);
        } catch (InvalidTenantIdentifierException ex) {
            throw new IllegalStateException("The '%s.tenants[%d].identifier' property is not a valid tenant identifier: %s"
                    .formatted(TenantDetailsProperties.CONFIG_PREFIX, index, ex.getMessage()), ex);
        }
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
