package io.arconia.multitenancy.web.context.filters;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

import io.arconia.multitenancy.web.autoconfigure.HttpTenantResolutionProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TenantContextIgnorePathMatcher}.
 */
class TenantContextIgnorePathMatcherTests {

    @Test
    void whenNullPathsThenThrow() {
        assertThatThrownBy(() -> new TenantContextIgnorePathMatcher(null)).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ignorePathPatterns cannot be null");
    }

    @Test
    void matchAgainstFullPath() {
        var request = new MockHttpServletRequest();
        request.setRequestURI("/actuator/prometheus");
        var matcher = new TenantContextIgnorePathMatcher(Set.of("/actuator/prometheus"));
        assertThat(matcher.matches(request)).isTrue();
    }

    @Test
    void matchAgainstFullPathWithoutTrailingSlash() {
        var request = new MockHttpServletRequest();
        request.setRequestURI("/actuator/prometheus");
        var matcher = new TenantContextIgnorePathMatcher(Set.of("actuator/prometheus"));
        assertThat(matcher.matches(request)).isTrue();
    }

    @Test
    void matchAgainstTemplatePath() {
        var request = new MockHttpServletRequest();
        request.setRequestURI("/actuator/prometheus");
        var matcher = new TenantContextIgnorePathMatcher(Set.of("/actuator/**"));
        assertThat(matcher.matches(request)).isTrue();
    }

    @Test
    void matchDifferentPathsThenFalse() {
        var request = new MockHttpServletRequest();
        request.setRequestURI("/actuators");
        var matcher = new TenantContextIgnorePathMatcher(Set.of("/actuator/**"));
        assertThat(matcher.matches(request)).isFalse();
    }

    @Test
    void whenNullRequestThenThrow() {
        var matcher = new TenantContextIgnorePathMatcher(Set.of("/actuator/**"));
        assertThatThrownBy(() -> matcher.matches(null)).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("httpServletRequest cannot be null");
    }

    @Test
    void matchAgainstPathWithinApplicationUnderContextPath() {
        var request = new MockHttpServletRequest();
        request.setContextPath("/app");
        request.setRequestURI("/app/actuator/health");
        var matcher = new TenantContextIgnorePathMatcher(Set.of("/actuator/**"));
        assertThat(matcher.matches(request)).isTrue();
    }

    @Test
    void whenContextPathIsRootThenPathIsUnchanged() {
        var request = new MockHttpServletRequest();
        request.setContextPath("");
        request.setRequestURI("/actuator/health");
        var matcher = new TenantContextIgnorePathMatcher(Set.of("/actuator/**"));
        assertThat(matcher.matches(request)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "/favicon.ico", "/static/images/favicon.ico", "/app.ico" })
    void shippedDefaultsIgnoreIconRequests(String requestUri) {
        var request = new MockHttpServletRequest();
        request.setRequestURI(requestUri);
        assertThat(shippedDefaults().matches(request)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "/actuator/health", "/webjars/bootstrap/css/bootstrap.css", "/css/app.css", "/js/app.js",
            "/login", "/oauth2/authorization/google", "/login/oauth2/code/google" })
    void shippedDefaultsIgnoreInfrastructureAndLoginPaths(String requestUri) {
        var request = new MockHttpServletRequest();
        request.setRequestURI(requestUri);
        assertThat(shippedDefaults().matches(request)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "/", "/api/orders", "/logins", "/oauth2", "/icons" })
    void shippedDefaultsDoNotIgnoreApplicationPaths(String requestUri) {
        var request = new MockHttpServletRequest();
        request.setRequestURI(requestUri);
        assertThat(shippedDefaults().matches(request)).isFalse();
    }

    private TenantContextIgnorePathMatcher shippedDefaults() {
        return new TenantContextIgnorePathMatcher(
                new HttpTenantResolutionProperties().getFilter().getIgnorePaths());
    }

}
