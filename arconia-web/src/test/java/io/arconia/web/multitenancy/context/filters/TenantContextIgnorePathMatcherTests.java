package io.arconia.web.multitenancy.context.filters;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

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
        var matcher = new TenantContextIgnorePathMatcher(List.of("/actuator/prometheus"));
        assertThat(matcher.matches(request)).isTrue();
    }

    @Test
    void matchAgainstFullPathWithoutTrailingSlash() {
        var request = new MockHttpServletRequest();
        request.setRequestURI("/actuator/prometheus");
        var matcher = new TenantContextIgnorePathMatcher(List.of("actuator/prometheus"));
        assertThat(matcher.matches(request)).isTrue();
    }

    @Test
    void matchAgainstTemplatePath() {
        var request = new MockHttpServletRequest();
        request.setRequestURI("/actuator/prometheus");
        var matcher = new TenantContextIgnorePathMatcher(List.of("/actuator/**"));
        assertThat(matcher.matches(request)).isTrue();
    }

    @Test
    void matchDifferentPathsThenFalse() {
        var request = new MockHttpServletRequest();
        request.setRequestURI("/actuators");
        var matcher = new TenantContextIgnorePathMatcher(List.of("/actuator/**"));
        assertThat(matcher.matches(request)).isFalse();
    }

    @Test
    void whenNullRequestThenThrow() {
        var matcher = new TenantContextIgnorePathMatcher(List.of("/actuator/**"));
        assertThatThrownBy(() -> matcher.matches(null)).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("httpServletRequest cannot be null");
    }

}
