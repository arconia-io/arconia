package io.arconia.multitenancy.core.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.multitenancy.core.tenantdetails.DefaultTenantVerifier;
import io.arconia.multitenancy.core.tenantdetails.TenantDetailsService;
import io.arconia.multitenancy.core.tenantdetails.TenantVerifier;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TenantDetailsProperties.class)
final class TenantDetailsConfiguration {

    @Bean
    @ConditionalOnMissingBean(TenantDetailsService.class)
    @ConditionalOnProperty(prefix = TenantDetailsProperties.CONFIG_PREFIX, name = "source", havingValue = "properties")
    PropertiesTenantDetailsService tenantDetailsService(TenantDetailsProperties tenantDetailsProperties) {
        return new PropertiesTenantDetailsService(tenantDetailsProperties);
    }

    @Bean
    @ConditionalOnMissingBean(TenantVerifier.class)
    @ConditionalOnBean(TenantDetailsService.class)
    DefaultTenantVerifier tenantVerifier(TenantDetailsService tenantDetailsService) {
        return new DefaultTenantVerifier(tenantDetailsService);
    }

}
