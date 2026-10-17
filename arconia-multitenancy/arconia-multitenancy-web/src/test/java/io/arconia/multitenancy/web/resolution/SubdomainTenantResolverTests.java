package io.arconia.multitenancy.web.resolution;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link SubdomainTenantResolver}.
 */
class SubdomainTenantResolverTests {

    @Test
    void whenNullPatternThenThrow() {
        assertThatIllegalArgumentException().isThrownBy(() -> SubdomainTenantResolver.builder().pattern(null).build())
            .withMessageContaining("pattern cannot be null or empty");
    }

    @Test
    void whenEmptyPatternThenThrow() {
        assertThatIllegalArgumentException().isThrownBy(() -> SubdomainTenantResolver.builder().pattern("").build())
            .withMessageContaining("pattern cannot be null or empty");
    }

    @Test
    void whenPatternHasNoTenantVariableThenThrow() {
        assertThatIllegalArgumentException().isThrownBy(() -> SubdomainTenantResolver.builder().pattern("rocks").build())
            .withMessageContaining("pattern must contain a {tenant} variable");
        assertThatIllegalArgumentException().isThrownBy(() -> SubdomainTenantResolver.builder().pattern("{id}.rocks").build())
            .withMessageContaining("pattern must contain a {tenant} variable");
    }

    @Test
    void whenPatternIsSetThenExposed() {
        assertThat(SubdomainTenantResolver.builder().pattern("{tenant}.rocks").build().getPattern())
            .isEqualTo("{tenant}.rocks");
    }

    @Test
    void whenHostMatchesPatternThenReturnTenant() {
        var resolver = SubdomainTenantResolver.builder().pattern("{tenant}.rocks").build();

        assertThat(resolver.resolveTenantIdentifier(requestFor("dukes.rocks"))).isEqualTo("dukes");
    }

    @Test
    void whenPatternHasMultipleLabelsThenReturnTenant() {
        var resolver = SubdomainTenantResolver.builder().pattern("{tenant}.app.example.com").build();

        assertThat(resolver.resolveTenantIdentifier(requestFor("dukes.app.example.com"))).isEqualTo("dukes");
    }

    @Test
    void whenVariableIsEmbeddedInLabelThenReturnTenant() {
        var resolver = SubdomainTenantResolver.builder().pattern("{tenant}-admin.myapp.rocks").build();

        assertThat(resolver.resolveTenantIdentifier(requestFor("dukes-admin.myapp.rocks"))).isEqualTo("dukes");
        assertThat(resolver.resolveTenantIdentifier(requestFor("dukes.myapp.rocks"))).isNull();
    }

    @Test
    void whenVariableIsNotFirstLabelThenReturnTenant() {
        var resolver = SubdomainTenantResolver.builder().pattern("admin.{tenant}.rocks").build();

        assertThat(resolver.resolveTenantIdentifier(requestFor("admin.dukes.rocks"))).isEqualTo("dukes");
    }

    @Test
    void whenVariableHasInlineRegexThenItIsApplied() {
        var resolver = SubdomainTenantResolver.builder().pattern("{tenant:[a-z]+}.rocks").build();

        assertThat(resolver.resolveTenantIdentifier(requestFor("dukes.rocks"))).isEqualTo("dukes");
        assertThat(resolver.resolveTenantIdentifier(requestFor("dukes1.rocks"))).isNull();
    }

    @Test
    void whenHostHasNestedSubdomainsThenReturnNull() {
        var resolver = SubdomainTenantResolver.builder().pattern("{tenant}.rocks").build();

        assertThat(resolver.resolveTenantIdentifier(requestFor("api.dukes.rocks"))).isNull();
    }

    @Test
    void whenHostDiffersInCaseThenMatchAndLowercase() {
        var resolver = SubdomainTenantResolver.builder().pattern("{tenant}.rocks").build();

        assertThat(resolver.resolveTenantIdentifier(requestFor("Dukes.ROCKS"))).isEqualTo("dukes");
    }

    @Test
    void whenPatternHasUppercaseLiteralsThenStillMatch() {
        var resolver = SubdomainTenantResolver.builder().pattern("{tenant}.MyApp.rocks").build();

        assertThat(resolver.resolveTenantIdentifier(requestFor("dukes.myapp.rocks"))).isEqualTo("dukes");
    }

    @Test
    void whenHostIsTheDomainItselfThenReturnNull() {
        var resolver = SubdomainTenantResolver.builder().pattern("{tenant}.rocks").build();

        assertThat(resolver.resolveTenantIdentifier(requestFor("rocks"))).isNull();
    }

    @Test
    void whenHostIsUnderAnotherDomainThenReturnNull() {
        var resolver = SubdomainTenantResolver.builder().pattern("{tenant}.rocks").build();

        assertThat(resolver.resolveTenantIdentifier(requestFor("dukes.example.com"))).isNull();
        assertThat(resolver.resolveTenantIdentifier(requestFor("dukesrocks"))).isNull();
        assertThat(resolver.resolveTenantIdentifier(requestFor("localhost"))).isNull();
    }

    @Test
    void whenHostIsEmptyThenReturnNull() {
        var resolver = SubdomainTenantResolver.builder().pattern("{tenant}.rocks").build();

        assertThat(resolver.resolveTenantIdentifier(requestFor(""))).isNull();
    }

    @Test
    void whenPortIsPresentThenItIsIgnored() {
        var resolver = SubdomainTenantResolver.builder().pattern("{tenant}.localhost").build();
        var request = requestFor("dukes.localhost");
        request.setServerPort(8080);

        assertThat(resolver.resolveTenantIdentifier(request)).isEqualTo("dukes");
    }

    @Test
    void whenNullRequestThenThrow() {
        var resolver = SubdomainTenantResolver.builder().pattern("{tenant}.rocks").build();

        assertThatIllegalArgumentException().isThrownBy(() -> resolver.resolveTenantIdentifier(null))
            .withMessageContaining("request cannot be null");
    }

    private static MockHttpServletRequest requestFor(String serverName) {
        var request = new MockHttpServletRequest();
        request.setServerName(serverName);
        return request;
    }

}
