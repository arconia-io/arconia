package io.arconia.autoconfigure.web.multitenancy;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.autoconfigure.core.multitenancy.FixedTenantResolutionProperties;
import io.arconia.core.multitenancy.context.resolvers.FixedTenantResolver;
import io.arconia.core.multitenancy.events.TenantEventPublisher;
import io.arconia.web.multitenancy.context.filters.TenantContextFilter;
import io.arconia.web.multitenancy.context.filters.TenantContextIgnorePathMatcher;
import io.arconia.web.multitenancy.context.resolvers.CookieTenantResolver;
import io.arconia.web.multitenancy.context.resolvers.HeaderTenantResolver;
import io.arconia.web.multitenancy.context.resolvers.HttpRequestTenantResolver;

/**
 * Configuration for HTTP tenant resolution.
 *
 * @author Thomas Vitale
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(HttpTenantResolutionProperties.class)
@ConditionalOnProperty(prefix = HttpTenantResolutionProperties.CONFIG_PREFIX, value = "enabled", havingValue = "true",
        matchIfMissing = true)
public class HttpTenantResolutionConfiguration {

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
            return new TenantContextIgnorePathMatcher(httpTenantResolutionProperties.getFilter().getIgnorePaths());
        }

    }

}
