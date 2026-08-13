package io.arconia.multitenancy.web.autoconfigure;

import java.util.HashSet;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;

import io.arconia.multitenancy.core.autoconfigure.FixedTenantResolutionProperties;
import io.arconia.multitenancy.core.context.resolvers.FixedTenantResolver;
import io.arconia.multitenancy.core.observability.TenantObservationFilter;
import io.arconia.multitenancy.core.tenantdetails.TenantIdentifierValidator;
import io.arconia.multitenancy.core.tenantdetails.TenantVerifier;
import io.arconia.multitenancy.web.context.filters.TenantContextFilter;
import io.arconia.multitenancy.web.context.filters.TenantContextIgnorePathMatcher;
import io.arconia.multitenancy.web.context.resolvers.CookieTenantResolver;
import io.arconia.multitenancy.web.context.resolvers.HeaderTenantResolver;
import io.arconia.multitenancy.web.context.resolvers.HttpRequestTenantResolver;
import io.arconia.multitenancy.web.context.resolvers.OAuth2TenantResolver;

/**
 * Configuration for HTTP tenant resolution.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(HttpTenantResolutionProperties.class)
@ConditionalOnBooleanProperty(prefix = HttpTenantResolutionProperties.CONFIG_PREFIX, value = "enabled",
        matchIfMissing = true)
public final class HttpTenantResolutionConfiguration {

    @Bean
    @ConditionalOnBean(FixedTenantResolver.class)
    @ConditionalOnBooleanProperty(prefix = FixedTenantResolutionProperties.CONFIG_PREFIX, value = "enabled")
    HttpRequestTenantResolver fixedHttpRequestTenantResolver(FixedTenantResolver fixedTenantResolver) {
        return fixedTenantResolver::resolveTenantIdentifier;
    }

    @Bean
    @ConditionalOnMissingBean(HttpRequestTenantResolver.class)
    @ConditionalOnProperty(prefix = HttpTenantResolutionProperties.CONFIG_PREFIX, value = "resolution-mode",
            havingValue = "header", matchIfMissing = true)
    HeaderTenantResolver headerTenantResolver(HttpTenantResolutionProperties httpTenantResolutionProperties) {
        return HeaderTenantResolver.builder()
            .tenantHeaderName(httpTenantResolutionProperties.getHeader().getHeaderName())
            .build();
    }

    @Bean
    @ConditionalOnMissingBean(HttpRequestTenantResolver.class)
    @ConditionalOnProperty(prefix = HttpTenantResolutionProperties.CONFIG_PREFIX, value = "resolution-mode",
            havingValue = "cookie")
    CookieTenantResolver cookieTenantResolver(HttpTenantResolutionProperties httpTenantResolutionProperties) {
        return CookieTenantResolver.builder()
            .tenantCookieName(httpTenantResolutionProperties.getCookie().getCookieName())
            .build();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(OAuth2AuthenticatedPrincipal.class)
    static class OAuth2TenantResolutionConfiguration {

        @Bean
        @ConditionalOnMissingBean(HttpRequestTenantResolver.class)
        @ConditionalOnProperty(prefix = HttpTenantResolutionProperties.CONFIG_PREFIX, value = "resolution-mode",
                havingValue = "oauth2")
        OAuth2TenantResolver oauth2TenantResolver(HttpTenantResolutionProperties httpTenantResolutionProperties) {
            return OAuth2TenantResolver.builder()
                .tenantClaimName(httpTenantResolutionProperties.getOauth2().getClaimName())
                .build();
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBooleanProperty(prefix = HttpTenantResolutionProperties.CONFIG_PREFIX, value = "filter.enabled",
            matchIfMissing = true)
    static class HttpTenantFilterConfiguration {

        @Bean
        @ConditionalOnMissingBean
        TenantContextFilter tenantContextFilter(HttpTenantResolutionProperties httpTenantResolutionProperties,
                HttpRequestTenantResolver httpRequestTenantResolver,
                TenantContextIgnorePathMatcher tenantContextIgnorePathMatcher,
                ApplicationEventPublisher eventPublisher, TenantIdentifierValidator tenantIdentifierValidator,
                ObjectProvider<TenantVerifier> tenantVerifier,
                ObjectProvider<TenantObservationFilter> tenantObservationFilter) {
            return TenantContextFilter.builder()
                .order(httpTenantResolutionProperties.getFilter().getOrder())
                .httpRequestTenantResolver(httpRequestTenantResolver)
                .tenantContextIgnorePathMatcher(tenantContextIgnorePathMatcher)
                .eventPublisher(eventPublisher)
                .tenantIdentifierValidator(tenantIdentifierValidator)
                .tenantVerifier(tenantVerifier.getIfAvailable())
                .tenantObservationFilter(tenantObservationFilter.getIfAvailable())
                .build();
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
