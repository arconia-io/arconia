package io.arconia.core.multitenancy.context;

import org.junit.jupiter.api.Test;

import io.arconia.core.multitenancy.exceptions.TenantNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantContextHolderTests {

    @Test
    void setValidTenantContext() {
        var tenantId = "acme";
        TenantContextHolder.setTenantId(tenantId);
        assertThat(TenantContextHolder.getTenantId()).isEqualTo(tenantId);
    }

    @Test
    void setNullTenantContext() {
        assertThatThrownBy(() -> TenantContextHolder.setTenantId(null)).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantId cannot be empty");
    }

    @Test
    void setEmptyTenantContext() {
        assertThatThrownBy(() -> TenantContextHolder.setTenantId("")).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantId cannot be empty");
    }

    @Test
    void clearTenantContext() {
        var tenantId = "acme";
        TenantContextHolder.setTenantId(tenantId);
        TenantContextHolder.clear();
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void whenRequiredTenantContextPresentThenReturn() {
        var tenantId = "acme";
        TenantContextHolder.setTenantId(tenantId);
        assertThat(TenantContextHolder.getRequiredTenantId()).isEqualTo(tenantId);
    }

    @Test
    void whenRequiredTenantContextMissingThenThrow() {
        assertThatThrownBy(TenantContextHolder::getRequiredTenantId).isInstanceOf(TenantNotFoundException.class)
            .hasMessageContaining("No tenant found in the current context");
    }

}
