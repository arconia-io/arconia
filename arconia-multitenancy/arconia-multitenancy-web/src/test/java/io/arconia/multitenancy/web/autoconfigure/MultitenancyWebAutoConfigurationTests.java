package io.arconia.multitenancy.web.autoconfigure;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializerBeans;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import io.arconia.multitenancy.core.autoconfigure.MultitenancyCoreAutoConfiguration;
import io.arconia.multitenancy.core.context.TenantContext;
import io.arconia.multitenancy.core.resolution.FixedTenantResolver;
import io.arconia.multitenancy.core.resolution.InvalidTenantIdentifierException;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MultitenancyWebAutoConfiguration}.
 */
class MultitenancyWebAutoConfigurationTests {

    private static final String RESOLUTION_MODE = "arconia.multitenancy.resolution.http.resolution-mode";

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
    void resolverChainDefaultsToHeader() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(HttpRequestTenantResolver.class);
            assertThat(configuredResolvers(context)).singleElement().isInstanceOf(HeaderTenantResolver.class);
            var request = new MockHttpServletRequest();
            request.addHeader("X-TenantId", "acme");
            assertThat(resolvedTenant(context, request)).isEqualTo("acme");
        });
    }

    @Test
    void resolverChainFromSingleMode() {
        contextRunner.withPropertyValues(RESOLUTION_MODE + "=cookie").run(context ->
            assertThat(configuredResolvers(context)).singleElement().isInstanceOf(CookieTenantResolver.class)
        );
    }

    @Test
    void resolverChainFromListOfModesKeepsOrder() {
        contextRunner
                .withPropertyValues(RESOLUTION_MODE + "=oauth2,subdomain,header",
                        "arconia.multitenancy.resolution.http.subdomain.pattern={tenant}.rocks")
                .run(context -> {
                    assertThat(configuredResolvers(context)).hasSize(3);
                    assertThat(configuredResolvers(context).get(0)).isInstanceOf(OAuth2TenantResolver.class);
                    assertThat(configuredResolvers(context).get(1)).isInstanceOf(SubdomainTenantResolver.class);
                    assertThat(configuredResolvers(context).get(2)).isInstanceOf(HeaderTenantResolver.class);
                });
    }

    @Test
    void resolverChainConsultsModesInOrder() {
        contextRunner
                .withPropertyValues(RESOLUTION_MODE + "=subdomain,header",
                        "arconia.multitenancy.resolution.http.subdomain.pattern={tenant}.rocks")
                .run(context -> {
                    var request = new MockHttpServletRequest();
                    request.setServerName("dukes.rocks");
                    request.addHeader("X-TenantId", "acme");
                    assertThat(resolvedTenant(context, request)).isEqualTo("dukes");

                    var fallbackRequest = new MockHttpServletRequest();
                    fallbackRequest.setServerName("localhost");
                    fallbackRequest.addHeader("X-TenantId", "acme");
                    assertThat(resolvedTenant(context, fallbackRequest)).isEqualTo("acme");
                });
    }

    @Test
    void resolverChainWithSubdomainModeWithoutPatternThenFail() {
        contextRunner.withPropertyValues(RESOLUTION_MODE + "=subdomain").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("arconia.multitenancy.resolution.http.subdomain.pattern")
                    .hasMessageContaining("'subdomain'");
        });
    }

    @Test
    void resolverChainWithPathMode() {
        contextRunner
                .withPropertyValues(RESOLUTION_MODE + "=path",
                        "arconia.multitenancy.resolution.http.path.pattern=/tenants/{tenant}/**")
                .run(context -> {
                    assertThat(configuredResolvers(context)).singleElement().isInstanceOf(PathTenantResolver.class);
                    var request = new MockHttpServletRequest("GET", "/tenants/acme/products");
                    request.setRequestURI("/tenants/acme/products");
                    assertThat(resolvedTenant(context, request)).isEqualTo("acme");
                });
    }

    @Test
    void resolverChainWithPathModeWithoutPatternThenFail() {
        contextRunner.withPropertyValues(RESOLUTION_MODE + "=path").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("arconia.multitenancy.resolution.http.path.pattern")
                    .hasMessageContaining("'path'");
        });
    }

    @Test
    void resolverChainWithFixedModeOnly() {
        contextRunner
                .withPropertyValues(RESOLUTION_MODE + "=fixed", "arconia.multitenancy.resolution.fixed.tenant-identifier=myTenant")
                .run(context -> {
                    assertThat(configuredResolvers(context)).singleElement().isInstanceOf(FixedTenantResolver.class);
                    assertThat(resolvedTenant(context, new MockHttpServletRequest())).isEqualTo("myTenant");
                });
    }

    @Test
    void resolverChainWithFixedModeLastActsAsDefaultTenant() {
        contextRunner
                .withPropertyValues(RESOLUTION_MODE + "=header,fixed",
                        "arconia.multitenancy.resolution.fixed.tenant-identifier=public")
                .run(context -> {
                    var request = new MockHttpServletRequest();
                    request.addHeader("X-TenantId", "acme");
                    assertThat(resolvedTenant(context, request)).isEqualTo("acme");
                    assertThat(resolvedTenant(context, new MockHttpServletRequest())).isEqualTo("public");
                });
    }

    @Test
    void resolverChainWithFixedModeNotLastThenFail() {
        contextRunner.withPropertyValues(RESOLUTION_MODE + "=fixed,header").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("'fixed' mode must be the last one");
        });
    }

    @Test
    void resolverChainWithInvalidFixedIdentifierThenFail() {
        contextRunner
                .withPropertyValues(RESOLUTION_MODE + "=fixed", "arconia.multitenancy.resolution.fixed.tenant-identifier=my tenant")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("arconia.multitenancy.resolution.fixed.tenant-identifier")
                            .hasStackTraceContaining("is not a valid tenant identifier")
                            .rootCause()
                            .isInstanceOf(InvalidTenantIdentifierException.class);
                });
    }

    @Test
    void resolverChainWithDuplicateModesThenFail() {
        contextRunner.withPropertyValues(RESOLUTION_MODE + "=header,cookie,header").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must not contain the same mode twice");
        });
    }

    @Test
    void resolverChainWithUnknownModeThenFail() {
        contextRunner.withPropertyValues(RESOLUTION_MODE + "=header,carrier-pigeon").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining(RESOLUTION_MODE);
        });
    }

    @Test
    void resolverChainWithOauth2Mode() {
        contextRunner
                .withPropertyValues(RESOLUTION_MODE + "=oauth2", "arconia.multitenancy.resolution.http.oauth2.claim-name=tid")
                .run(context -> {
                    assertThat(configuredResolvers(context)).singleElement().isInstanceOf(OAuth2TenantResolver.class);
                    var idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(60),
                            Map.of("sub", "user", "tid", "myTenant"));
                    var securityContext = SecurityContextHolder.createEmptyContext();
                    securityContext.setAuthentication(new OAuth2AuthenticationToken(
                            new DefaultOidcUser(AuthorityUtils.NO_AUTHORITIES, idToken), List.of(), "keycloak"));
                    SecurityContextHolder.setContext(securityContext);
                    try {
                        assertThat(resolvedTenant(context, new MockHttpServletRequest())).isEqualTo("myTenant");
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                });
    }

    @Test
    void resolverChainWithOauth2ModeWithoutSpringSecurityThenFail() {
        contextRunner.withClassLoader(new FilteredClassLoader(OAuth2AuthenticatedPrincipal.class))
                .withPropertyValues(RESOLUTION_MODE + "=oauth2")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).rootCause()
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("Spring Security OAuth2 is not on the classpath");
                });
    }

    @Test
    void resolverChainWithEmptyModesThenFail() {
        contextRunner.withPropertyValues(RESOLUTION_MODE + "=").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("At least one mode must be set");
        });
    }

    @Test
    void customResolverReplacesConfiguredChain() {
        contextRunner.withBean("customTenantResolver", HttpRequestTenantResolver.class, () -> request -> "customTenant")
                .run(context -> {
                    assertThat(context).hasSingleBean(HttpRequestTenantResolver.class);
                    assertThat(context).doesNotHaveBean(CompositeHttpRequestTenantResolver.class);
                    var request = new MockHttpServletRequest();
                    request.addHeader("X-TenantId", "acme");
                    assertThat(resolvedTenant(context, request)).isEqualTo("customTenant");
                });
    }

    @Test
    void customCompositeResolverCombinesCustomAndBuiltInResolvers() {
        contextRunner.withUserConfiguration(CompositeResolverConfiguration.class).run(context -> {
            var request = new MockHttpServletRequest();
            request.addHeader("X-TenantId", "acme");
            assertThat(resolvedTenant(context, request)).isEqualTo("acme");
            var queryRequest = new MockHttpServletRequest();
            queryRequest.setParameter("tenant", "beans");
            assertThat(resolvedTenant(context, queryRequest)).isEqualTo("beans");
        });
    }

    @Test
    void tenantResolutionFailureHandlerDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(TenantResolutionFailureHandler.class);
            assertThat(context).hasSingleBean(ProblemDetailTenantResolutionFailureHandler.class);
        });
    }

    @Test
    void tenantResolutionFailureHandlerCustom() {
        contextRunner.withBean("customFailureHandler", TenantResolutionFailureHandler.class,
                () -> (request, response, failure) -> response.sendRedirect("/choose-tenant"))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ProblemDetailTenantResolutionFailureHandler.class);
                    var response = new MockHttpServletResponse();
                    context.getBean(TenantContextFilter.class).doFilter(new MockHttpServletRequest(), response, new MockFilterChain());
                    assertThat(response.getRedirectedUrl()).isEqualTo("/choose-tenant");
                });
    }

    @Test
    void tenantContextFilterDefault() {
        contextRunner.run(context ->
            assertThat(context).hasSingleBean(TenantContextFilter.class)
        );
    }

    @Test
    void tenantContextFilterOrderDefault() {
        contextRunner.run(context ->
            assertThat(context.getBean(TenantContextFilter.class).getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE)
        );
    }

    @Test
    void tenantContextFilterOrderCustom() {
        contextRunner.withPropertyValues("arconia.multitenancy.resolution.http.filter.order=-101").run(context ->
            assertThat(context.getBean(TenantContextFilter.class).getOrder()).isEqualTo(-101)
        );
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

    private static List<?> configuredResolvers(AssertableWebApplicationContext context) {
        return context.getBean(CompositeHttpRequestTenantResolver.class).getResolvers();
    }

    /**
     * Runs the auto-configured filter and returns the tenant bound while the chain runs,
     * or the status of the failure response when no tenant was bound.
     */
    private static String resolvedTenant(AssertableWebApplicationContext context, MockHttpServletRequest request)
            throws ServletException, IOException {
        var response = new MockHttpServletResponse();
        var filterChain = new MockFilterChain(new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse res) throws IOException {
                res.getWriter().write(String.valueOf(TenantContext.getTenantIdentifier()));
            }
        });
        context.getBean(TenantContextFilter.class).doFilter(request, response, filterChain);
        return response.getStatus() == 200 ? response.getContentAsString() : "status:" + response.getStatus();
    }

    @Configuration(proxyBeanMethods = false)
    static class CompositeResolverConfiguration {

        @Bean
        HttpRequestTenantResolver tenantResolver() {
            HttpRequestTenantResolver queryParameterTenantResolver = request -> request.getParameter("tenant");
            return new CompositeHttpRequestTenantResolver(
                    List.of(HeaderTenantResolver.builder().build(), queryParameterTenantResolver));
        }

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
