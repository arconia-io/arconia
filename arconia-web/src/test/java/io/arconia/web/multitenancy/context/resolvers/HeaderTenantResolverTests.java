package io.arconia.web.multitenancy.context.resolvers;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link HeaderTenantResolver}.
 */
class HeaderTenantResolverTests {

    @Test
    void whenNullCustomHeaderThenThrow() {
        assertThatThrownBy(() -> new HeaderTenantResolver(null)).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantHeaderName cannot be null or empty");
    }

    @Test
    void whenEmptyCustomHeaderThenThrow() {
        assertThatThrownBy(() -> new HeaderTenantResolver("")).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantHeaderName cannot be null or empty");
    }

    @Test
    void whenDefaultHeaderIsUsed() {
        var expectedTenantId = "default";
        var headerTenantResolver = new HeaderTenantResolver();
        var request = new MockHttpServletRequest();
        request.addHeader(HeaderTenantResolver.DEFAULT_HEADER_NAME, expectedTenantId);

        var actualTenantId = headerTenantResolver.resolveTenantIdentifier(request);

        assertThat(actualTenantId).isEqualTo(expectedTenantId);
    }

    @Test
    void whenCustomHeaderIsUsed() {
        var expectedTenantId = "default";
        var headerName = "tenantIdentifier";
        var headerTenantResolver = new HeaderTenantResolver(headerName);
        var request = new MockHttpServletRequest();
        request.addHeader(headerName, expectedTenantId);

        var actualTenantId = headerTenantResolver.resolveTenantIdentifier(request);

        assertThat(actualTenantId).isEqualTo(expectedTenantId);
    }

    @Test
    void whenNullRequestThenThrow() {
        var headerTenantResolver = new HeaderTenantResolver();

        assertThatThrownBy(() -> headerTenantResolver.resolveTenantIdentifier(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("request cannot be null");
    }

}
