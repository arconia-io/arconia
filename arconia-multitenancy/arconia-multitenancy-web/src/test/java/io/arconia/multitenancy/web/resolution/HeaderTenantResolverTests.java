package io.arconia.multitenancy.web.resolution;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import io.arconia.multitenancy.core.resolution.InvalidTenantIdentifierException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link HeaderTenantResolver}.
 */
class HeaderTenantResolverTests {

    @Test
    void whenNullCustomHeaderThenThrow() {
        assertThatIllegalArgumentException().isThrownBy(() -> HeaderTenantResolver.builder().tenantHeaderName(null).build())
            .withMessageContaining("tenantHeaderName cannot be null or empty");
    }

    @Test
    void whenEmptyCustomHeaderThenThrow() {
        assertThatIllegalArgumentException().isThrownBy(() -> HeaderTenantResolver.builder().tenantHeaderName("").build())
            .withMessageContaining("tenantHeaderName cannot be null or empty");
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
    void whenMultipleHeadersThenThrow() {
        var headerTenantResolver = HeaderTenantResolver.builder().build();
        var request = new MockHttpServletRequest();
        request.addHeader("X-TenantId", "acme");
        request.addHeader("X-TenantId", "beans");

        assertThatThrownBy(() -> headerTenantResolver.resolveTenantIdentifier(request))
            .isInstanceOf(InvalidTenantIdentifierException.class)
            .hasMessageContaining("must not contain more than one 'X-TenantId' header");
    }

    @Test
    void whenNullRequestThenThrow() {
        var headerTenantResolver = HeaderTenantResolver.builder().build();

        assertThatIllegalArgumentException().isThrownBy(() -> headerTenantResolver.resolveTenantIdentifier(null))
            .withMessageContaining("request cannot be null");
    }

}
