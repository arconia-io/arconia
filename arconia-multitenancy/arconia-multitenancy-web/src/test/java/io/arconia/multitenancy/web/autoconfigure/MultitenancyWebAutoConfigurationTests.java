package io.arconia.multitenancy.web.autoconfigure;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializerBeans;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import io.arconia.multitenancy.core.autoconfigure.MultitenancyCoreAutoConfiguration;
import io.arconia.multitenancy.web.context.filters.TenantContextFilter;
import io.arconia.multitenancy.web.context.filters.TenantContextIgnorePathMatcher;
import io.arconia.multitenancy.web.context.resolvers.CookieTenantResolver;
import io.arconia.multitenancy.web.context.resolvers.HeaderTenantResolver;
import io.arconia.multitenancy.web.context.resolvers.HttpRequestTenantResolver;
import io.arconia.multitenancy.web.context.resolvers.OAuth2TenantResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MultitenancyWebAutoConfiguration}.
 */
class MultitenancyWebAutoConfigurationTests {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner().withConfiguration(
            AutoConfigurations.of(MultitenancyCoreAutoConfiguration.class, MultitenancyWebAutoConfiguration.class));

    @Test
    void whenNoServletContextThenBackOff() {
        var nonServletContextRunner = new ApplicationContextRunner().withConfiguration(
                AutoConfigurations.of(MultitenancyCoreAutoConfiguration.class, MultitenancyWebAutoConfiguration.class));

        nonServletContextRunner
                .run(context -> assertThat(context).doesNotHaveBean(HttpTenantResolutionConfiguration.class));
    }

    @Test
    void httpTenantResolutionDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(HttpTenantResolutionConfiguration.class);
        });
    }

    @Test
    void httpTenantResolutionDisabled() {
        contextRunner.withPropertyValues("arconia.multitenancy.resolution.http.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(HttpTenantResolutionConfiguration.class));
    }

    @Test
    void httpRequestTenantResolverDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(HeaderTenantResolver.class);
        });
    }

    @Test
    void httpRequestTenantResolverCookie() {
        contextRunner.withPropertyValues("arconia.multitenancy.resolution.http.resolution-mode=cookie").run(context -> {
            assertThat(context).hasSingleBean(CookieTenantResolver.class);
        });
    }

    @Test
    void httpRequestTenantResolverFixed() {
        contextRunner
                .withPropertyValues("arconia.multitenancy.resolution.fixed.enabled=true",
                        "arconia.multitenancy.resolution.fixed.tenant-identifier=myTenant")
                .run(context -> {
                    assertThat(context).hasSingleBean(HttpRequestTenantResolver.class);
                    var httpRequestTenantResolver = context.getBean(HttpRequestTenantResolver.class);
                    assertThat(httpRequestTenantResolver.resolveTenantIdentifier(new MockHttpServletRequest()))
                            .isEqualTo("myTenant");
                });
    }

    @Test
    void httpRequestTenantResolverOauth2() {
        contextRunner.withPropertyValues("arconia.multitenancy.resolution.http.resolution-mode=oauth2").run(context -> {
            assertThat(context).hasSingleBean(OAuth2TenantResolver.class);
        });
    }

    @Test
    void httpRequestTenantResolverOauth2CustomClaim() {
        contextRunner
                .withPropertyValues("arconia.multitenancy.resolution.http.resolution-mode=oauth2",
                        "arconia.multitenancy.resolution.http.oauth2.claim-name=tid")
                .run(context -> {
                    var oauth2TenantResolver = context.getBean(OAuth2TenantResolver.class);
                    var idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(60),
                            Map.of("sub", "user", "tid", "myTenant"));
                    var securityContext = SecurityContextHolder.createEmptyContext();
                    securityContext.setAuthentication(new OAuth2AuthenticationToken(
                            new DefaultOidcUser(AuthorityUtils.NO_AUTHORITIES, idToken), List.of(), "keycloak"));
                    SecurityContextHolder.setContext(securityContext);
                    try {
                        assertThat(oauth2TenantResolver.resolveTenantIdentifier(new MockHttpServletRequest()))
                                .isEqualTo("myTenant");
                    }
                    finally {
                        SecurityContextHolder.clearContext();
                    }
                });
    }

    @Test
    void httpRequestTenantResolverCustom() {
        contextRunner.withPropertyValues("arconia.multitenancy.resolution.http.resolution-mode=oauth2")
                .withBean(HttpRequestTenantResolver.class, () -> request -> "customTenant")
                .run(context -> {
                    assertThat(context).hasSingleBean(HttpRequestTenantResolver.class);
                    assertThat(context).doesNotHaveBean(OAuth2TenantResolver.class);
                });
    }

    @Test
    void tenantContextFilterDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(TenantContextFilter.class);
        });
    }

    @Test
    void tenantContextFilterOrderDefault() {
        contextRunner.run(context -> {
            assertThat(context.getBean(TenantContextFilter.class).getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
        });
    }

    @Test
    void tenantContextFilterOrderCustom() {
        contextRunner.withPropertyValues("arconia.multitenancy.resolution.http.filter.order=-101").run(context -> {
            assertThat(context.getBean(TenantContextFilter.class).getOrder()).isEqualTo(-101);
        });
    }

    @Test
    void tenantContextFilterRegisteredOnceWithConfiguredOrder() {
        contextRunner.withPropertyValues("arconia.multitenancy.resolution.http.filter.order=-101").run(context -> {
            var registrations = filterRegistrations(context.getBeanFactory());
            assertThat(registrations).singleElement().satisfies(registration -> {
                assertThat(registration.getFilter()).isSameAs(context.getBean(TenantContextFilter.class));
                assertThat(registration.getOrder()).isEqualTo(-101);
            });
        });
    }

    @Test
    void tenantContextFilterRegistrationCanBeDisabled() {
        contextRunner.withUserConfiguration(CustomFilterRegistrationConfiguration.class).run(context -> {
            var registrations = filterRegistrations(context.getBeanFactory());
            assertThat(registrations).singleElement().satisfies(registration -> {
                assertThat(registration.getFilter()).isSameAs(context.getBean(TenantContextFilter.class));
                assertThat(registration.isEnabled()).isFalse();
            });
        });
    }

    private static List<FilterRegistrationBean> filterRegistrations(ListableBeanFactory beanFactory) {
        return StreamSupport.stream(new ServletContextInitializerBeans(beanFactory).spliterator(), false)
                .filter(FilterRegistrationBean.class::isInstance)
                .map(FilterRegistrationBean.class::cast)
                .toList();
    }

    @Test
    void tenantContextIgnorePathMatcher() {
        contextRunner
                .withPropertyValues("arconia.multitenancy.resolution.http.filter.ignore-paths=/actuator/**,/status")
                .run(context -> {
                    assertThat(context).hasSingleBean(TenantContextIgnorePathMatcher.class);
                    var tenantContextIgnorePathMatcher = context.getBean(TenantContextIgnorePathMatcher.class);
                    var mockRequest = new MockHttpServletRequest();
                    mockRequest.setRequestURI("/actuator/prometheus");
                    assertThat(tenantContextIgnorePathMatcher.matches(mockRequest)).isTrue();
                });
    }

    @Test
    void tenantContextFilterDisabled() {
        contextRunner.withPropertyValues("arconia.multitenancy.resolution.http.filter.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(TenantContextFilter.class));
    }

    @Test
    void tenantContextIgnorePathMatcherDisabled() {
        contextRunner.withPropertyValues("arconia.multitenancy.resolution.http.filter.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(TenantContextIgnorePathMatcher.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomFilterRegistrationConfiguration {

        @Bean
        FilterRegistrationBean<TenantContextFilter> tenantContextFilterRegistration(TenantContextFilter filter) {
            var registration = new FilterRegistrationBean<>(filter);
            registration.setEnabled(false);
            return registration;
        }

    }

}
