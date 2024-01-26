package io.arconia.web.multitenancy.context.resolvers;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CookieTenantResolverTests {

    @Test
    void whenNullCustomCookieThenThrow() {
        assertThatThrownBy(() -> new CookieTenantResolver(null)).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantCookieName cannot be empty");
    }

    @Test
    void whenEmptyCustomCookieThenThrow() {
        assertThatThrownBy(() -> new CookieTenantResolver("")).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantCookieName cannot be empty");
    }

    @Test
    void whenDefaultCookieIsUsed() {
        var expectedTenantId = "default";
        var cookieTenantResolver = new CookieTenantResolver();
        var request = new MockHttpServletRequest();
        request.setCookies(new Cookie(CookieTenantResolver.DEFAULT_COOKIE_NAME, expectedTenantId));

        var actualTenantId = cookieTenantResolver.resolveTenantId(request);

        assertThat(actualTenantId).isEqualTo(expectedTenantId);
    }

    @Test
    void whenCustomCookieIsUsed() {
        var expectedTenantId = "default";
        var cookieName = "tenantIdentifier";
        var cookieTenantResolver = new CookieTenantResolver(cookieName);
        var request = new MockHttpServletRequest();
        request.setCookies(new Cookie(cookieName, expectedTenantId));

        var actualTenantId = cookieTenantResolver.resolveTenantId(request);

        assertThat(actualTenantId).isEqualTo(expectedTenantId);
    }

    @Test
    void whenNullRequestThenThrow() {
        var cookieTenantResolver = new CookieTenantResolver();

        assertThatThrownBy(() -> cookieTenantResolver.resolveTenantId(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("request cannot be null");
    }

}
