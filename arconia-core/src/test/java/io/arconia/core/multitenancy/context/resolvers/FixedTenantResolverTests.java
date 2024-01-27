package io.arconia.core.multitenancy.context.resolvers;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link FixedTenantResolver}.
 *
 * @author Thomas Vitale
 */
class FixedTenantResolverTests {

    @Test
    void whenNullCustomValueThenThrow() {
        assertThatThrownBy(() -> new FixedTenantResolver(null)).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantIdentifier cannot be empty");
    }

    @Test
    void whenEmptyCustomValueThenThrow() {
        assertThatThrownBy(() -> new FixedTenantResolver("")).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantIdentifier cannot be empty");
    }

    @Test
    void whenDefaultIsUsedAsFixedTenant() {
        var expectedTenantIdentifier = "default";
        var fixedTenantResolver = new FixedTenantResolver();
        var actualTenantIdentifier = fixedTenantResolver.resolveTenantIdentifier(this);
        assertThat(actualTenantIdentifier).isEqualTo(expectedTenantIdentifier);
    }

    @Test
    void whenCustomValueIsUsedAsFixedTenant() {
        var expectedTenantIdentifier = "beans";
        var fixedTenantResolver = new FixedTenantResolver(expectedTenantIdentifier);
        var actualTenantIdentifier = fixedTenantResolver.resolveTenantIdentifier(this);
        assertThat(actualTenantIdentifier).isEqualTo(expectedTenantIdentifier);
    }

}
