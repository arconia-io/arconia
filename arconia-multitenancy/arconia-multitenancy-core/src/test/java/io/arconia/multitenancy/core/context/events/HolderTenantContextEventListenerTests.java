package io.arconia.multitenancy.core.context.events;

import org.junit.jupiter.api.Test;

import io.arconia.multitenancy.core.context.TenantContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link HolderTenantContextEventListener}.
 */
class HolderTenantContextEventListenerTests {

    @Test
    void whenContextIsAttached() {
        var listener = new HolderTenantContextEventListener();
        var tenantIdentifier = "acme";

        listener.onApplicationEvent(new TenantContextAttachedEvent(tenantIdentifier, this));

        assertThat(TenantContextHolder.getTenantIdentifier()).isEqualTo(tenantIdentifier);
    }

    @Test
    void whenContextIsClosed() {
        var listener = new HolderTenantContextEventListener();
        var tenantIdentifier = "acme";

        listener.onApplicationEvent(new TenantContextAttachedEvent(tenantIdentifier, this));
        listener.onApplicationEvent(new TenantContextClosedEvent(tenantIdentifier, this));

        assertThat(TenantContextHolder.getTenantIdentifier()).isNull();
    }

}
