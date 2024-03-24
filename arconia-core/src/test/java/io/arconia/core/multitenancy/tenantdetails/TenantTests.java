package io.arconia.core.multitenancy.tenantdetails;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link Tenant}.
 */
class TenantTests {

    @Test
    void whenIdentifierIsNullThenThrow() {
        assertThatThrownBy(() -> Tenant.create().build()).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("identifier cannot be null or empty");
    }

    @Test
    void whenIdentifierIsEmptyThenThrow() {
        assertThatThrownBy(() -> Tenant.create().identifier("").build()).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("identifier cannot be null or empty");
    }

    @Test
    void whenAttributesIsNullThenUseEmptymap() {
        var tenant = Tenant.create().identifier("acme").attributes(null).build();
        assertThat(tenant.getAttributes()).isNotNull();
        assertThat(tenant.getAttributes()).isEmpty();
    }

}
