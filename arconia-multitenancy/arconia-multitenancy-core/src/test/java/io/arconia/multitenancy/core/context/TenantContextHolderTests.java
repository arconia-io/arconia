package io.arconia.multitenancy.core.context;

import org.junit.jupiter.api.Test;

import io.arconia.multitenancy.core.exceptions.TenantNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TenantContextHolder}.
 */
class TenantContextHolderTests {

    @Test
    void setValidTenantContext() {
        var tenantIdentifier = "acme";
        TenantContextHolder.setTenantIdentifier(tenantIdentifier);
        assertThat(TenantContextHolder.getTenantIdentifier()).isEqualTo(tenantIdentifier);
    }

    @Test
    void setNullTenantContext() {
        assertThatThrownBy(() -> TenantContextHolder.setTenantIdentifier(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenant cannot be null or empty");
    }

    @Test
    void setEmptyTenantContext() {
        assertThatThrownBy(() -> TenantContextHolder.setTenantIdentifier(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenant cannot be null or empty");
    }

    @Test
    void clearTenantContext() {
        var tenantIdentifier = "acme";
        TenantContextHolder.setTenantIdentifier(tenantIdentifier);
        TenantContextHolder.clear();
        assertThat(TenantContextHolder.getTenantIdentifier()).isNull();
    }

    @Test
    void whenRequiredTenantContextPresentThenReturn() {
        var tenantIdentifier = "acme";
        TenantContextHolder.setTenantIdentifier(tenantIdentifier);
        assertThat(TenantContextHolder.getRequiredTenantIdentifier()).isEqualTo(tenantIdentifier);
    }

    @Test
    void whenRequiredTenantContextMissingThenThrow() {
        assertThatThrownBy(TenantContextHolder::getRequiredTenantIdentifier).isInstanceOf(TenantNotFoundException.class)
            .hasMessageContaining("No tenant found in the current context");
    }

}
