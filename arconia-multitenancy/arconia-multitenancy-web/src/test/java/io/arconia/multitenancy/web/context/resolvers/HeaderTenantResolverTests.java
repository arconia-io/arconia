package io.arconia.multitenancy.web.context.resolvers;

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
        assertThatThrownBy(() -> HeaderTenantResolver.builder().tenantHeaderName(null).build()).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantHeaderName cannot be null or empty");
    }

    @Test
    void whenEmptyCustomHeaderThenThrow() {
        assertThatThrownBy(() -> HeaderTenantResolver.builder().tenantHeaderName("").build()).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantHeaderName cannot be null or empty");
    }

    @Test
    void whenDefaultHeaderIsUsed() {
        var expectedTenantId = "default";
        var headerTenantResolver = HeaderTenantResolver.builder().build();
        var request = new MockHttpServletRequest();
        request.addHeader("X-TenantId", expectedTenantId);

        var actualTenantId = headerTenantResolver.resolveTenantIdentifier(request);

        assertThat(actualTenantId).isEqualTo(expectedTenantId);
    }

    @Test
    void whenCustomHeaderIsUsed() {
        var expectedTenantId = "default";
        var headerName = "tenantIdentifier";
        var headerTenantResolver = HeaderTenantResolver.builder().tenantHeaderName(headerName).build();
        var request = new MockHttpServletRequest();
        request.addHeader(headerName, expectedTenantId);

        var actualTenantId = headerTenantResolver.resolveTenantIdentifier(request);

        assertThat(actualTenantId).isEqualTo(expectedTenantId);
    }

    @Test
    void whenHeaderMissingThenReturnNull() {
        var headerTenantResolver = HeaderTenantResolver.builder().build();
        var request = new MockHttpServletRequest();

        var actualTenantId = headerTenantResolver.resolveTenantIdentifier(request);

        assertThat(actualTenantId).isNull();
    }

    @Test
    void whenNullRequestThenThrow() {
        var headerTenantResolver = HeaderTenantResolver.builder().build();

        assertThatThrownBy(() -> headerTenantResolver.resolveTenantIdentifier(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("request cannot be null");
    }

}
