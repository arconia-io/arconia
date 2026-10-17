package io.arconia.multitenancy.web.resolution;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link PathTenantResolver}.
 */
class PathTenantResolverTests {

    @Test
    void whenNullPatternThenThrow() {
        assertThatIllegalArgumentException().isThrownBy(() -> PathTenantResolver.builder().pattern(null).build())
            .withMessageContaining("pattern cannot be null or empty");
    }

    @Test
    void whenEmptyPatternThenThrow() {
        assertThatIllegalArgumentException().isThrownBy(() -> PathTenantResolver.builder().pattern("").build())
            .withMessageContaining("pattern cannot be null or empty");
    }

    @Test
    void whenPatternHasNoTenantVariableThenThrow() {
        assertThatIllegalArgumentException().isThrownBy(() -> PathTenantResolver.builder().pattern("/tenants/{id}/**").build())
            .withMessageContaining("pattern must contain a {tenant} variable");
    }

    @Test
    void whenPatternIsSetThenExposed() {
        var resolver = PathTenantResolver.builder().pattern("/tenants/{tenant}/**").build();

        assertThat(resolver.getPattern()).isEqualTo("/tenants/{tenant}/**");
    }

    @Test
    void whenPathMatchesPatternThenReturnTenant() {
        var resolver = PathTenantResolver.builder().pattern("/tenants/{tenant}/**").build();

        assertThat(resolver.resolveTenantIdentifier(request("/tenants/acme/products"))).isEqualTo("acme");
    }

    @Test
    void whenPathIsExactlyTheTenantSegmentThenReturnTenant() {
        var resolver = PathTenantResolver.builder().pattern("/tenants/{tenant}/**").build();

        assertThat(resolver.resolveTenantIdentifier(request("/tenants/acme"))).isEqualTo("acme");
    }

    @Test
    void whenVariableIsFirstSegmentThenReturnTenant() {
        var resolver = PathTenantResolver.builder().pattern("/{tenant}/api/**").build();

        assertThat(resolver.resolveTenantIdentifier(request("/acme/api/products"))).isEqualTo("acme");
    }

    @Test
    void whenVariableHasInlineRegexThenItIsApplied() {
        var resolver = PathTenantResolver.builder().pattern("/tenants/{tenant:[a-z]+}/**").build();

        assertThat(resolver.resolveTenantIdentifier(request("/tenants/acme/products"))).isEqualTo("acme");
        assertThat(resolver.resolveTenantIdentifier(request("/tenants/acme1/products"))).isNull();
    }

    @Test
    void whenPathDoesNotMatchPatternThenReturnNull() {
        var resolver = PathTenantResolver.builder().pattern("/tenants/{tenant}/**").build();

        assertThat(resolver.resolveTenantIdentifier(request("/products"))).isNull();
        assertThat(resolver.resolveTenantIdentifier(request("/tenants"))).isNull();
    }

    @Test
    void whenContextPathIsSetThenMatchedWithinApplication() {
        var resolver = PathTenantResolver.builder().pattern("/tenants/{tenant}/**").build();
        var request = request("/app/tenants/acme/products");
        request.setContextPath("/app");

        assertThat(resolver.resolveTenantIdentifier(request)).isEqualTo("acme");
    }

    @Test
    void whenPatternHasNoLeadingSlashThenItIsAdded() {
        var resolver = PathTenantResolver.builder().pattern("tenants/{tenant}/**").build();

        assertThat(resolver.resolveTenantIdentifier(request("/tenants/acme/products"))).isEqualTo("acme");
    }

    @Test
    void whenPathIsEncodedThenVariableIsDecoded() {
        var resolver = PathTenantResolver.builder().pattern("/tenants/{tenant}/**").build();

        assertThat(resolver.resolveTenantIdentifier(request("/tenants/acme%20corp/products"))).isEqualTo("acme corp");
    }

    @Test
    void whenPathIsCaseDifferentThenNoMatch() {
        var resolver = PathTenantResolver.builder().pattern("/tenants/{tenant}/**").build();

        assertThat(resolver.resolveTenantIdentifier(request("/Tenants/acme/products"))).isNull();
    }

    @Test
    void whenNullRequestThenThrow() {
        var resolver = PathTenantResolver.builder().pattern("/tenants/{tenant}/**").build();

        assertThatIllegalArgumentException().isThrownBy(() -> resolver.resolveTenantIdentifier(null))
            .withMessageContaining("request cannot be null");
    }

    private static MockHttpServletRequest request(String uri) {
        var request = new MockHttpServletRequest("GET", uri);
        request.setRequestURI(uri);
        return request;
    }

}
