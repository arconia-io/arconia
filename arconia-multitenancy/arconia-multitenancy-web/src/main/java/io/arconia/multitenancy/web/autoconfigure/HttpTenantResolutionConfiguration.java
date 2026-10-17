package io.arconia.multitenancy.web.autoconfigure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import io.arconia.multitenancy.core.autoconfigure.FixedTenantResolutionProperties;
import io.arconia.multitenancy.core.observability.TenantObservationFilter;
import io.arconia.multitenancy.core.resolution.FixedTenantResolver;
import io.arconia.multitenancy.core.resolution.InvalidTenantIdentifierException;
import io.arconia.multitenancy.core.resolution.TenantIdentifierValidator;
import io.arconia.multitenancy.core.resolution.TenantResolver;
import io.arconia.multitenancy.core.resolution.TenantVerifier;
import io.arconia.multitenancy.web.autoconfigure.HttpTenantResolutionProperties.HttpResolutionMode;
import io.arconia.multitenancy.web.context.TenantContextFilter;
import io.arconia.multitenancy.web.context.TenantContextIgnorePathMatcher;
import io.arconia.multitenancy.web.resolution.CompositeHttpRequestTenantResolver;
import io.arconia.multitenancy.web.resolution.CookieTenantResolver;
import io.arconia.multitenancy.web.resolution.HeaderTenantResolver;
import io.arconia.multitenancy.web.resolution.HttpRequestTenantResolver;
import io.arconia.multitenancy.web.resolution.OAuth2TenantResolver;
import io.arconia.multitenancy.web.resolution.PathTenantResolver;
import io.arconia.multitenancy.web.resolution.ProblemDetailTenantResolutionFailureHandler;
import io.arconia.multitenancy.web.resolution.SubdomainTenantResolver;
import io.arconia.multitenancy.web.resolution.TenantResolutionFailureHandler;

/**
 * Configuration for HTTP tenant resolution.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(HttpTenantResolutionProperties.class)
@ConditionalOnBooleanProperty(prefix = HttpTenantResolutionProperties.CONFIG_PREFIX, value = "enabled", matchIfMissing = true)
public final class HttpTenantResolutionConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(HttpTenantResolutionConfiguration.class);

    private static final String RESOLUTION_MODE_PROPERTY = HttpTenantResolutionProperties.CONFIG_PREFIX + ".resolution-mode";

    private static final String OAUTH2_PRINCIPAL_CLASS = "org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal";

    /**
     * The resolver chain selected by the configured resolution modes, consulted in the
     * configured order. A user-defined {@link HttpRequestTenantResolver} bean replaces it.
     */
    @Bean
    @ConditionalOnMissingBean(HttpRequestTenantResolver.class)
    CompositeHttpRequestTenantResolver httpRequestTenantResolver(
            HttpTenantResolutionProperties httpTenantResolutionProperties,
            FixedTenantResolutionProperties fixedTenantResolutionProperties,
            TenantIdentifierValidator tenantIdentifierValidator,
            ResourceLoader resourceLoader
    ) {
        var modes = httpTenantResolutionProperties.getResolutionMode();
        Assert.state(!modes.isEmpty(), "At least one mode must be set in '" + RESOLUTION_MODE_PROPERTY + "'");
        Assert.state(new HashSet<>(modes).size() == modes.size(),
                "The '" + RESOLUTION_MODE_PROPERTY + "' property must not contain the same mode twice");
        var fixedIndex = modes.indexOf(HttpResolutionMode.FIXED);
        Assert.state(fixedIndex < 0 || fixedIndex == modes.size() - 1,
                "The 'fixed' mode must be the last one in '" + RESOLUTION_MODE_PROPERTY
                        + "', since it always resolves a tenant and any mode after it would never be consulted");
        var resolvers = new ArrayList<TenantResolver<? super HttpServletRequest>>();
        for (var mode : modes) {
            resolvers.add(createResolver(mode, httpTenantResolutionProperties, fixedTenantResolutionProperties,
                    tenantIdentifierValidator, resourceLoader.getClassLoader()));
        }
        logger.info("Resolving the tenant for HTTP requests from: {}",
                modes.stream().map(mode -> mode.name().toLowerCase(Locale.ROOT)).collect(Collectors.joining(", ")));
        return new CompositeHttpRequestTenantResolver(resolvers);
    }

    private static TenantResolver<? super HttpServletRequest> createResolver(HttpResolutionMode mode,
            HttpTenantResolutionProperties properties, FixedTenantResolutionProperties fixedProperties,
            TenantIdentifierValidator tenantIdentifierValidator, @Nullable ClassLoader classLoader
    ) {
        return switch (mode) {
            case HEADER -> HeaderTenantResolver.builder().tenantHeaderName(properties.getHeader().getHeaderName()).build();
            case COOKIE -> CookieTenantResolver.builder().tenantCookieName(properties.getCookie().getCookieName()).build();
            case PATH -> {
                var pattern = properties.getPath().getPattern();
                Assert.state(pattern != null && !pattern.isBlank(), "The '" + HttpTenantResolutionProperties.CONFIG_PREFIX
                        + ".path.pattern' property must be set when the 'path' mode is in '" + RESOLUTION_MODE_PROPERTY + "'");
                yield PathTenantResolver.builder().pattern(pattern).build();
            }
            case SUBDOMAIN -> {
                var pattern = properties.getSubdomain().getPattern();
                Assert.state(pattern != null && !pattern.isBlank(), "The '" + HttpTenantResolutionProperties.CONFIG_PREFIX
                        + ".subdomain.pattern' property must be set when the 'subdomain' mode is in '" + RESOLUTION_MODE_PROPERTY + "'");
                yield SubdomainTenantResolver.builder().pattern(pattern).build();
            }
            case OAUTH2 -> {
                Assert.state(ClassUtils.isPresent(OAUTH2_PRINCIPAL_CLASS, classLoader),
                        "The 'oauth2' mode is in '" + RESOLUTION_MODE_PROPERTY
                                + "', but Spring Security OAuth2 is not on the classpath. Add it to your project, for example with the"
                                + " 'spring-boot-starter-security-oauth2-resource-server' or 'spring-boot-starter-security-oauth2-client' dependency,"
                                + " or use another resolution mode");
                yield OAuth2Support.createResolver(properties.getOauth2().getClaimName());
            }
            case FIXED -> {
                var tenantIdentifier = fixedProperties.getTenantIdentifier();
                try {
                    tenantIdentifierValidator.validate(tenantIdentifier);
                } catch (InvalidTenantIdentifierException ex) {
                    throw new IllegalStateException("The '" + FixedTenantResolutionProperties.CONFIG_PREFIX
                            + ".tenant-identifier' property is not a valid tenant identifier: " + ex.getMessage(), ex);
                }
                yield FixedTenantResolver.builder().tenantIdentifier(tenantIdentifier).build();
            }
        };
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBooleanProperty(prefix = HttpTenantResolutionProperties.CONFIG_PREFIX, value = "filter.enabled", matchIfMissing = true)
    static class HttpTenantFilterConfiguration {

        @Bean
        @ConditionalOnMissingBean
        TenantResolutionFailureHandler tenantResolutionFailureHandler() {
            return new ProblemDetailTenantResolutionFailureHandler();
        }

        @Bean
        @ConditionalOnMissingBean
        TenantContextFilter tenantContextFilter(
                HttpTenantResolutionProperties httpTenantResolutionProperties,
                HttpRequestTenantResolver httpRequestTenantResolver,
                TenantContextIgnorePathMatcher tenantContextIgnorePathMatcher,
                ApplicationEventPublisher eventPublisher,
                TenantIdentifierValidator tenantIdentifierValidator,
                ObjectProvider<TenantVerifier> tenantVerifier,
                ObjectProvider<TenantObservationFilter> tenantObservationFilter,
                TenantResolutionFailureHandler tenantResolutionFailureHandler
        ) {
            return TenantContextFilter.builder()
                .order(httpTenantResolutionProperties.getFilter().getOrder())
                .httpRequestTenantResolver(httpRequestTenantResolver)
                .tenantContextIgnorePathMatcher(tenantContextIgnorePathMatcher)
                .eventPublisher(eventPublisher)
                .tenantIdentifierValidator(tenantIdentifierValidator)
                .tenantVerifier(tenantVerifier.getIfAvailable())
                .tenantObservationFilter(tenantObservationFilter.getIfAvailable())
                .failureHandler(tenantResolutionFailureHandler)
                .build();
        }

        @Bean
        @ConditionalOnMissingBean
        TenantContextIgnorePathMatcher tenantContextIgnorePathMatcher(HttpTenantResolutionProperties httpTenantResolutionProperties) {
            var ignorePathMatcher = new HashSet<>(httpTenantResolutionProperties.getFilter().getIgnorePaths());
            ignorePathMatcher.addAll(httpTenantResolutionProperties.getFilter().getAdditionalIgnorePaths());
            return new TenantContextIgnorePathMatcher(ignorePathMatcher);
        }

    }

    /**
     * Keeps the Spring Security OAuth2 types out of the enclosing class
     * so that it loads when they are absent from the classpath.
     */
    private static final class OAuth2Support {

        private OAuth2Support() {}

        static HttpRequestTenantResolver createResolver(String claimName) {
            return OAuth2TenantResolver.builder().tenantClaimName(claimName).build();
        }

    }

}
