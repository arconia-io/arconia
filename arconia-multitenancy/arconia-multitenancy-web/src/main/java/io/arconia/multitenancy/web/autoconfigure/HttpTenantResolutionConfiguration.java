package io.arconia.multitenancy.web.autoconfigure;

import java.util.HashSet;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.multitenancy.core.autoconfigure.FixedTenantResolutionProperties;
import io.arconia.multitenancy.core.context.resolvers.FixedTenantResolver;
import io.arconia.multitenancy.core.events.TenantEventPublisher;
import io.arconia.multitenancy.web.context.filters.TenantContextFilter;
import io.arconia.multitenancy.web.context.filters.TenantContextIgnorePathMatcher;
import io.arconia.multitenancy.web.context.resolvers.CookieTenantResolver;
import io.arconia.multitenancy.web.context.resolvers.HeaderTenantResolver;
import io.arconia.multitenancy.web.context.resolvers.HttpRequestTenantResolver;

/**
 * Configuration for HTTP tenant resolution.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(HttpTenantResolutionProperties.class)
@ConditionalOnProperty(prefix = HttpTenantResolutionProperties.CONFIG_PREFIX, value = "enabled", havingValue = "true",
        matchIfMissing = true)
public final class HttpTenantResolutionConfiguration {

    private HttpTenantResolutionConfiguration() {}

    @Bean
    @ConditionalOnBean(FixedTenantResolver.class)
    @ConditionalOnProperty(prefix = FixedTenantResolutionProperties.CONFIG_PREFIX, value = "enabled",
            havingValue = "true")
    HttpRequestTenantResolver fixedHttpRequestTenantResolver(FixedTenantResolver fixedTenantResolver) {
        return fixedTenantResolver::resolveTenantIdentifier;
    }

    @Bean
    @ConditionalOnMissingBean(HttpRequestTenantResolver.class)
    @ConditionalOnProperty(prefix = HttpTenantResolutionProperties.CONFIG_PREFIX, value = "resolution-mode",
            havingValue = "header", matchIfMissing = true)
    HeaderTenantResolver headerTenantResolver(HttpTenantResolutionProperties httpTenantResolutionProperties) {
        return new HeaderTenantResolver(httpTenantResolutionProperties.getHeader().getHeaderName());
    }

    @Bean
    @ConditionalOnMissingBean(HttpRequestTenantResolver.class)
    @ConditionalOnProperty(prefix = HttpTenantResolutionProperties.CONFIG_PREFIX, value = "resolution-mode",
            havingValue = "cookie")
    CookieTenantResolver cookieTenantResolver(HttpTenantResolutionProperties httpTenantResolutionProperties) {
        return new CookieTenantResolver(httpTenantResolutionProperties.getCookie().getCookieName());
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = HttpTenantResolutionProperties.CONFIG_PREFIX, value = "filter.enabled",
            havingValue = "true", matchIfMissing = true)
    static class HttpTenantFilterConfiguration {

        @Bean
        @ConditionalOnMissingBean
        TenantContextFilter tenantContextFilter(HttpRequestTenantResolver httpRequestTenantResolver,
                TenantContextIgnorePathMatcher tenantContextIgnorePathMatcher,
                TenantEventPublisher tenantEventPublisher) {
            return new TenantContextFilter(httpRequestTenantResolver, tenantContextIgnorePathMatcher,
                    tenantEventPublisher);
        }

        @Bean
        @ConditionalOnMissingBean
        TenantContextIgnorePathMatcher tenantContextIgnorePathMatcher(
                HttpTenantResolutionProperties httpTenantResolutionProperties) {
            var ignorePathMatcher = new HashSet<>(httpTenantResolutionProperties.getFilter().getIgnorePaths());
            ignorePathMatcher.addAll(httpTenantResolutionProperties.getFilter().getAdditionalIgnorePaths());
            return new TenantContextIgnorePathMatcher(ignorePathMatcher);
        }

    }

}
