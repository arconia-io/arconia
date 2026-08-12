package io.arconia.multitenancy.core.tenantdetails;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link Tenant}.
 */
class TenantTests {

    @Test
    void whenIdentifierIsNullThenThrow() {
        assertThatThrownBy(() -> Tenant.builder().build()).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("identifier cannot be null or empty");
    }

    @Test
    void whenIdentifierIsEmptyThenThrow() {
        assertThatThrownBy(() -> Tenant.builder().identifier("").build()).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("identifier cannot be null or empty");
    }

    @Test
    void whenAttributesIsNullThenThrow() {
        assertThatThrownBy(() -> Tenant.builder().identifier("acme").attributes(null).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("attributes cannot be null");
    }

}
