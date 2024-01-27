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
            .hasMessageContaining("tenantName cannot be empty");
    }

    @Test
    void whenEmptyCustomValueThenThrow() {
        assertThatThrownBy(() -> new FixedTenantResolver("")).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantName cannot be empty");
    }

    @Test
    void whenDefaultIsUsedAsFixedTenant() {
        var expectedTenantId = "default";
        var fixedTenantResolver = new FixedTenantResolver();
        var actualTenantId = fixedTenantResolver.resolveTenantId(this);
        assertThat(actualTenantId).isEqualTo(expectedTenantId);
    }

    @Test
    void whenCustomValueIsUsedAsFixedTenant() {
        var expectedTenantId = "beans";
        var fixedTenantResolver = new FixedTenantResolver(expectedTenantId);
        var actualTenantId = fixedTenantResolver.resolveTenantId(this);
        assertThat(actualTenantId).isEqualTo(expectedTenantId);
    }

}
