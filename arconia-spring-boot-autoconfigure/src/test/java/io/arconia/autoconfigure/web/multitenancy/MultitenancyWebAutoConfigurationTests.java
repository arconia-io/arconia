package io.arconia.autoconfigure.web.multitenancy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.mock.web.MockHttpServletRequest;

import io.arconia.autoconfigure.core.multitenancy.MultitenancyCoreAutoConfiguration;
import io.arconia.web.multitenancy.context.filters.TenantContextFilter;
import io.arconia.web.multitenancy.context.filters.TenantContextIgnorePathMatcher;
import io.arconia.web.multitenancy.context.resolvers.CookieTenantResolver;
import io.arconia.web.multitenancy.context.resolvers.HeaderTenantResolver;
import io.arconia.web.multitenancy.context.resolvers.HttpRequestTenantResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MultitenancyWebAutoConfiguration}.
 *
 * @author Thomas Vitale
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
                    "arconia.multitenancy.resolution.fixed.tenant-id=myTenant")
            .run(context -> {
                assertThat(context).hasSingleBean(HttpRequestTenantResolver.class);
                var httpRequestTenantResolver = context.getBean(HttpRequestTenantResolver.class);
                assertThat(httpRequestTenantResolver.resolveTenantId(new MockHttpServletRequest()))
                    .isEqualTo("myTenant");
            });
    }

    @Test
    void tenantContextFilterDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(TenantContextFilter.class);
        });
    }

    @Test
    void tenantContextIgnorePathMatcher() {
        contextRunner
            .withPropertyValues("arconia.multitenancy.resolution.http.filter.ignore-paths=/actuator/**,/status")
            .run(context -> {
                assertThat(context).hasSingleBean(TenantContextIgnorePathMatcher.class);
                var tenatContextIgnorePathMatcher = context.getBean(TenantContextIgnorePathMatcher.class);
                var mockRequest = new MockHttpServletRequest();
                mockRequest.setRequestURI("/actuator/prometheus");
                assertThat(tenatContextIgnorePathMatcher.matches(mockRequest)).isTrue();
            });
    }

    @Test
    void tenantContextFilterDisabled() {
        contextRunner.withPropertyValues("arconia.multitenancy.resolution.http.filter.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(NoSuchBeanDefinitionException.class));
    }

    @Test
    void tenantContextIgnorePathMatcherDisabled() {
        contextRunner.withPropertyValues("arconia.multitenancy.resolution.http.filter.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(TenantContextIgnorePathMatcher.class));
    }

}
