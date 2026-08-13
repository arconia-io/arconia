package io.arconia.multitenancy.web.context.resolvers;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link CookieTenantResolver}.
 */
class CookieTenantResolverTests {

    @Test
    void whenNullCustomCookieThenThrow() {
        assertThatThrownBy(() -> CookieTenantResolver.builder().tenantCookieName(null).build()).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantCookieName cannot be null or empty");
    }

    @Test
    void whenEmptyCustomCookieThenThrow() {
        assertThatThrownBy(() -> CookieTenantResolver.builder().tenantCookieName("").build()).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantCookieName cannot be null or empty");
    }

    @Test
    void whenDefaultCookieIsUsed() {
        var expectedTenantId = "default";
        var cookieTenantResolver = CookieTenantResolver.builder().build();
        var request = new MockHttpServletRequest();
        request.setCookies(new Cookie("TENANT-ID", expectedTenantId));

        var actualTenantId = cookieTenantResolver.resolveTenantIdentifier(request);

        assertThat(actualTenantId).isEqualTo(expectedTenantId);
    }

    @Test
    void whenCustomCookieIsUsed() {
        var expectedTenantId = "default";
        var cookieName = "tenantIdentifier";
        var cookieTenantResolver = CookieTenantResolver.builder().tenantCookieName(cookieName).build();
        var request = new MockHttpServletRequest();
        request.setCookies(new Cookie(cookieName, expectedTenantId));

        var actualTenantId = cookieTenantResolver.resolveTenantIdentifier(request);

        assertThat(actualTenantId).isEqualTo(expectedTenantId);
    }

    @Test
    void whenNoCookiesPresentThenReturnNull() {
        var cookieTenantResolver = CookieTenantResolver.builder().build();
        var request = new MockHttpServletRequest();

        var actualTenantId = cookieTenantResolver.resolveTenantIdentifier(request);

        assertThat(actualTenantId).isNull();
    }

    @Test
    void whenNullRequestThenThrow() {
        var cookieTenantResolver = CookieTenantResolver.builder().build();

        assertThatThrownBy(() -> cookieTenantResolver.resolveTenantIdentifier(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("request cannot be null");
    }

}
