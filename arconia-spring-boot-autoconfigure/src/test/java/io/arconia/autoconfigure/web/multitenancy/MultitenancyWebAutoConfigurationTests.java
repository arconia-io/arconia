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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
            .run(context -> assertThatThrownBy(() -> context.getBean(HttpTenantResolutionConfiguration.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class));
    }

    @Test
    void httpTenantResolutionDefault() {
        contextRunner.run(context -> {
            var bean = context.getBean(HttpTenantResolutionConfiguration.class);
            assertThat(bean).isInstanceOf(HttpTenantResolutionConfiguration.class);
        });
    }

    @Test
    void httpTenantResolutionDisabled() {
        contextRunner.withPropertyValues("arconia.multitenancy.resolution.http.enabled=false")
            .run(context -> assertThatThrownBy(() -> context.getBean(HttpTenantResolutionConfiguration.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class));
    }

    @Test
    void httpRequestTenantResolverDefault() {
        contextRunner.run(context -> {
            var bean = context.getBean(HttpRequestTenantResolver.class);
            assertThat(bean).isInstanceOf(HeaderTenantResolver.class);
        });
    }

    @Test
    void httpRequestTenantResolverCookie() {
        contextRunner.withPropertyValues("arconia.multitenancy.resolution.http.resolution-mode=cookie").run(context -> {
            var bean = context.getBean(HttpRequestTenantResolver.class);
            assertThat(bean).isInstanceOf(CookieTenantResolver.class);
        });
    }

    @Test
    void httpRequestTenantResolverFixed() {
        contextRunner
            .withPropertyValues("arconia.multitenancy.resolution.fixed.enabled=true",
                    "arconia.multitenancy.resolution.fixed.tenant-id=myTenant")
            .run(context -> {
                var bean = context.getBean(HttpRequestTenantResolver.class);
                assertThat(bean).isInstanceOf(HttpRequestTenantResolver.class);
                assertThat(bean.resolveTenantId(new MockHttpServletRequest())).isEqualTo("myTenant");
            });
    }

    @Test
    void tenantContextFilterDefault() {
        contextRunner.run(context -> {
            var bean = context.getBean(TenantContextFilter.class);
            assertThat(bean).isInstanceOf(TenantContextFilter.class);
        });
    }

    @Test
    void tenantContextIgnorePathMatcher() {
        contextRunner
            .withPropertyValues("arconia.multitenancy.resolution.http.filter.ignore-paths=/actuator/**,/status")
            .run(context -> {
                var bean = context.getBean(TenantContextIgnorePathMatcher.class);
                assertThat(bean).isInstanceOf(TenantContextIgnorePathMatcher.class);
                var mockRequest = new MockHttpServletRequest();
                mockRequest.setRequestURI("/actuator/prometheus");
                assertThat(bean.matches(mockRequest)).isTrue();
            });
    }

    @Test
    void tenantContextFilterDisabled() {
        contextRunner.withPropertyValues("arconia.multitenancy.resolution.http.filter.enabled=false")
            .run(context -> assertThatThrownBy(() -> context.getBean(TenantContextFilter.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class));
    }

    @Test
    void tenantContextIgnorePathMatcherDisabled() {
        contextRunner.withPropertyValues("arconia.multitenancy.resolution.http.filter.enabled=false")
            .run(context -> assertThatThrownBy(() -> context.getBean(TenantContextIgnorePathMatcher.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class));
    }

}
