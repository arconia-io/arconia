package io.arconia.multitenancy.core.context.resolvers;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link FixedTenantResolver}.
 */
class FixedTenantResolverTests {

    @Test
    void whenNullCustomValueThenThrow() {
        assertThatThrownBy(() -> FixedTenantResolver.builder().tenantIdentifier(null).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantIdentifier cannot be null or empty");
    }

    @Test
    void whenEmptyCustomValueThenThrow() {
        assertThatThrownBy(() -> FixedTenantResolver.builder().tenantIdentifier("").build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantIdentifier cannot be null or empty");
    }

    @Test
    void whenDefaultIsUsedAsFixedTenant() {
        var fixedTenantResolver = FixedTenantResolver.builder().build();

        assertThat(fixedTenantResolver.resolveTenantIdentifier(this)).isEqualTo("default");
    }

    @Test
    void whenCustomValueIsUsedAsFixedTenant() {
        var expectedTenantIdentifier = "beans";
        var fixedTenantResolver = FixedTenantResolver.builder().tenantIdentifier(expectedTenantIdentifier).build();

        assertThat(fixedTenantResolver.resolveTenantIdentifier(this)).isEqualTo(expectedTenantIdentifier);
    }

}
