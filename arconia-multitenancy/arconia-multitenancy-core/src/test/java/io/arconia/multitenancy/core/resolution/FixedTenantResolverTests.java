package io.arconia.multitenancy.core.resolution;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link FixedTenantResolver}.
 */
class FixedTenantResolverTests {

    @Test
    void whenNullCustomValueThenThrow() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> FixedTenantResolver.builder().tenantIdentifier(null).build())
            .withMessageContaining("tenantIdentifier cannot be null or empty");
    }

    @Test
    void whenEmptyCustomValueThenThrow() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> FixedTenantResolver.builder().tenantIdentifier("").build())
            .withMessageContaining("tenantIdentifier cannot be null or empty");
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
