package io.arconia.multitenancy.web.resolution;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import io.arconia.multitenancy.core.resolution.FixedTenantResolver;
import io.arconia.multitenancy.core.resolution.InvalidTenantIdentifierException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link CompositeHttpRequestTenantResolver}.
 */
class CompositeHttpRequestTenantResolverTests {

    @Test
    void whenEmptyResolversThenThrow() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new CompositeHttpRequestTenantResolver(List.of()))
            .withMessageContaining("resolvers cannot be null or empty");
    }

    @Test
    void whenNullResolversThenThrow() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new CompositeHttpRequestTenantResolver(null))
            .withMessageContaining("resolvers cannot be null or empty");
    }

    @Test
    void whenResolverReturnsEmptyValueThenNextOneIsConsulted() {
        HttpRequestTenantResolver empty = request -> "";
        var resolver = new CompositeHttpRequestTenantResolver(List.of(empty,
                FixedTenantResolver.builder().tenantIdentifier("public").build()));

        assertThat(resolver.resolveTenantIdentifier(new MockHttpServletRequest())).isEqualTo("public");
    }

    @Test
    void whenNoResolverResolvesThenNull() {
        HttpRequestTenantResolver nothing = request -> null;

        assertThat(new CompositeHttpRequestTenantResolver(List.of(nothing)).resolveTenantIdentifier(new MockHttpServletRequest()))
            .isNull();
    }

    @Test
    void whenNullRequestThenThrow() {
        var resolver = new CompositeHttpRequestTenantResolver(List.of(HeaderTenantResolver.builder().build()));

        assertThatIllegalArgumentException()
            .isThrownBy(() -> resolver.resolveTenantIdentifier(null))
            .withMessageContaining("request cannot be null");
    }

    @Test
    void whenFirstResolverHandsOverThenNextOneResolves() {
        var resolver = new CompositeHttpRequestTenantResolver(List.of(HeaderTenantResolver.builder().build(),
                FixedTenantResolver.builder().tenantIdentifier("public").build()));
        var request = new MockHttpServletRequest();

        assertThat(resolver.resolveTenantIdentifier(request)).isEqualTo("public");

        request.addHeader("X-TenantId", "acme");
        assertThat(resolver.resolveTenantIdentifier(request)).isEqualTo("acme");
    }

    @Test
    void whenResolverThrowsThenChainStops() {
        var resolver = new CompositeHttpRequestTenantResolver(List.of(HeaderTenantResolver.builder().build(),
                FixedTenantResolver.builder().tenantIdentifier("public").build()));
        var request = new MockHttpServletRequest();
        request.addHeader("X-TenantId", "acme");
        request.addHeader("X-TenantId", "beans");

        assertThatThrownBy(() -> resolver.resolveTenantIdentifier(request))
            .isInstanceOf(InvalidTenantIdentifierException.class);
    }

    @Test
    void whenBuiltThenResolversAreExposedInOrder() {
        var header = HeaderTenantResolver.builder().build();
        var cookie = CookieTenantResolver.builder().build();

        assertThat(new CompositeHttpRequestTenantResolver(List.of(header, cookie)).getResolvers())
            .containsExactly(header, cookie);
    }

}
