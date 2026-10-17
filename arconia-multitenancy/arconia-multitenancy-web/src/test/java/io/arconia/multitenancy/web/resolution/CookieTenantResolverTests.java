package io.arconia.multitenancy.web.resolution;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import io.arconia.multitenancy.core.resolution.InvalidTenantIdentifierException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link CookieTenantResolver}.
 */
class CookieTenantResolverTests {

    @Test
    void whenNullCustomCookieThenThrow() {
        assertThatIllegalArgumentException().isThrownBy(() -> CookieTenantResolver.builder().tenantCookieName(null).build())
            .withMessageContaining("tenantCookieName cannot be null or empty");
    }

    @Test
    void whenEmptyCustomCookieThenThrow() {
        assertThatIllegalArgumentException().isThrownBy(() -> CookieTenantResolver.builder().tenantCookieName("").build())
            .withMessageContaining("tenantCookieName cannot be null or empty");
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
    void whenMultipleCookiesWithSameNameThenThrow() {
        var cookieTenantResolver = CookieTenantResolver.builder().build();
        var request = new MockHttpServletRequest();
        request.setCookies(new Cookie("TENANT-ID", "acme"), new Cookie("TENANT-ID", "beans"));

        assertThatThrownBy(() -> cookieTenantResolver.resolveTenantIdentifier(request))
            .isInstanceOf(InvalidTenantIdentifierException.class)
            .hasMessageContaining("must not contain more than one 'TENANT-ID' cookie");
    }

    @Test
    void whenNullRequestThenThrow() {
        var cookieTenantResolver = CookieTenantResolver.builder().build();

        assertThatIllegalArgumentException().isThrownBy(() -> cookieTenantResolver.resolveTenantIdentifier(null))
            .withMessageContaining("request cannot be null");
    }

}
