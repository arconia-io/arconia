package io.arconia.multitenancy.core.autoconfigure;

import java.time.Duration;
import java.util.ArrayList;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import io.arconia.multitenancy.core.resolution.DefaultTenantVerifier;
import io.arconia.multitenancy.core.resolution.InvalidTenantIdentifierException;
import io.arconia.multitenancy.core.resolution.TenantIdentifierValidator;
import io.arconia.multitenancy.core.resolution.TenantVerifier;
import io.arconia.multitenancy.core.tenantdetails.CachingTenantDetailsService;
import io.arconia.multitenancy.core.tenantdetails.InMemoryTenantDetailsService;
import io.arconia.multitenancy.core.tenantdetails.Tenant;
import io.arconia.multitenancy.core.tenantdetails.TenantDetailsService;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TenantDetailsProperties.class)
final class TenantDetailsConfiguration {

    private static final String CAFFEINE_CLASS = "com.github.benmanes.caffeine.cache.Caffeine";

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
    DefaultTenantVerifier tenantVerifier(TenantDetailsService tenantDetailsService,
            TenantDetailsProperties tenantDetailsProperties, ResourceLoader resourceLoader) {
        return new DefaultTenantVerifier(cached(tenantDetailsService, tenantDetailsProperties.getCache(),
                resourceLoader.getClassLoader()));
    }

    /**
     * Only the verifier goes through the cache, since it looks up a tenant on every
     * request, while other consumers such as the tenants endpoint keep reading live data.
     */
    private static TenantDetailsService cached(TenantDetailsService tenantDetailsService,
            TenantDetailsProperties.Cache cache, @Nullable ClassLoader classLoader) {
        var timeToLive = cache.getTimeToLive();
        if (timeToLive == null) {
            return tenantDetailsService;
        }
        Assert.state(ClassUtils.isPresent(CAFFEINE_CLASS, classLoader), "The '" + TenantDetailsProperties.CONFIG_PREFIX
                + ".cache.time-to-live' property is set, but Caffeine is not on the classpath. Add the"
                + " 'com.github.ben-manes.caffeine:caffeine' dependency to your project, or remove the property");
        return CaffeineSupport.createService(tenantDetailsService, timeToLive, cache.getMaxSize());
    }

    /**
     * Keeps the Caffeine-backed service out of the enclosing class so that it loads when
     * Caffeine is absent from the classpath.
     */
    private static final class CaffeineSupport {

        private CaffeineSupport() {}

        static TenantDetailsService createService(TenantDetailsService tenantDetailsService, Duration timeToLive,
                long maxSize) {
            return CachingTenantDetailsService.builder()
                .delegate(tenantDetailsService)
                .timeToLive(timeToLive)
                .maxSize(maxSize)
                .build();
        }

    }

}
