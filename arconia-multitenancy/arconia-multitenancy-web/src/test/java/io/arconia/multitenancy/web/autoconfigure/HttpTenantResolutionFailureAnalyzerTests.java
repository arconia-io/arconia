package io.arconia.multitenancy.web.autoconfigure;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.core.Ordered;
import org.springframework.mock.env.MockEnvironment;

import io.arconia.multitenancy.web.context.resolvers.HttpRequestTenantResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link HttpTenantResolutionFailureAnalyzer}.
 */
class HttpTenantResolutionFailureAnalyzerTests {

    private static final String RESOLUTION_MODE_PROPERTY = HttpTenantResolutionProperties.CONFIG_PREFIX
            + ".resolution-mode";

    @Test
    void whenOAuth2ModeAndResolverMissingThenAnalyzed() {
        var analysis = analyze("oauth2", new NoSuchBeanDefinitionException(HttpRequestTenantResolver.class));

        assertThat(analysis).isNotNull();
        assertThat(analysis.getDescription()).contains("is set to 'oauth2'")
            .contains("Spring Security OAuth2 is not on the classpath");
        assertThat(analysis.getAction()).contains("spring-boot-starter-security-oauth2-resource-server")
            .contains("set %s to 'header' or 'cookie'".formatted(RESOLUTION_MODE_PROPERTY));
    }

    @Test
    void whenOAuth2ModeIsUppercaseThenStillAnalyzed() {
        assertThat(analyze("OAUTH2", new NoSuchBeanDefinitionException(HttpRequestTenantResolver.class))).isNotNull();
    }

    @Test
    void whenResolutionModeIsNotOAuth2ThenNotAnalyzed() {
        assertThat(analyze("header", new NoSuchBeanDefinitionException(HttpRequestTenantResolver.class))).isNull();
    }

    @Test
    void whenResolutionModeIsNotSetThenNotAnalyzed() {
        assertThat(analyze(null, new NoSuchBeanDefinitionException(HttpRequestTenantResolver.class))).isNull();
    }

    @Test
    void whenAnotherBeanTypeIsMissingThenNotAnalyzed() {
        assertThat(analyze("oauth2", new NoSuchBeanDefinitionException(String.class))).isNull();
    }

    @Test
    void runsBeforeOtherFailureAnalyzers() {
        assertThat(new HttpTenantResolutionFailureAnalyzer(new MockEnvironment()).getOrder())
            .isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }

    @Nullable
    private FailureAnalysis analyze(@Nullable String resolutionMode, NoSuchBeanDefinitionException cause) {
        var environment = new MockEnvironment();
        if (resolutionMode != null) {
            environment.setProperty(RESOLUTION_MODE_PROPERTY, resolutionMode);
        }
        return new HttpTenantResolutionFailureAnalyzer(environment).analyze(cause);
    }

}
