package io.arconia.multitenancy.autoconfigure.core.tenantdetails;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.multitenancy.core.context.events.ValidatingTenantContextEventListener;
import io.arconia.multitenancy.core.tenantdetails.TenantDetailsService;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TenantDetailsProperties.class)
public class TenantDetailsConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = TenantDetailsProperties.CONFIG_PREFIX, name = "source", havingValue = "properties")
    TenantDetailsService tenantDetailsService(TenantDetailsProperties tenantDetailsProperties) {
        return new PropertiesTenantDetailsService(tenantDetailsProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(TenantDetailsService.class)
    ValidatingTenantContextEventListener validatingTenantContextEventListener(
            TenantDetailsService tenantDetailsService) {
        return new ValidatingTenantContextEventListener(tenantDetailsService);
    }

}
