package io.arconia.core.multitenancy.context.events;

import org.junit.jupiter.api.Test;

import io.arconia.core.multitenancy.context.TenantContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class HolderTenantContextEventListenerTests {

    @Test
    void whenContextIsAttached() {
        var listener = new HolderTenantContextEventListener();
        var tenantId = "acme";

        listener.onApplicationEvent(new TenantContextAttachedEvent(tenantId, this));

        assertThat(TenantContextHolder.getTenantId()).isEqualTo(tenantId);
    }

    @Test
    void whenContextIsClosed() {
        var listener = new HolderTenantContextEventListener();
        var tenantId = "acme";

        listener.onApplicationEvent(new TenantContextAttachedEvent(tenantId, this));
        listener.onApplicationEvent(new TenantContextClosedEvent(tenantId, this));

        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

}
