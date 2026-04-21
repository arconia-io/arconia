package io.arconia.multitenancy.core.observability;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import io.arconia.multitenancy.core.context.events.TenantContextAttachedEvent;
import io.arconia.multitenancy.core.context.events.TenantContextClosedEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link MdcTenantEventListener}.
 */
class MdcTenantEventListenerTests {

    @Test
    void whenNullCustomValueThenThrow() {
        assertThatThrownBy(() -> new MdcTenantEventListener(null)).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantIdentifierKey cannot be null or empty");
    }

    @Test
    void whenEmptyCustomValueThenThrow() {
        assertThatThrownBy(() -> new MdcTenantEventListener("")).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantIdentifierKey cannot be null or empty");
    }

    @Test
    void whenDefaultValueIsUsedAsKey() {
        var tenantKey = "tenantId";
        var tenantValue = "acme";
        var listener = new MdcTenantEventListener();

        listener.onAttached(new TenantContextAttachedEvent(tenantValue, this));

        assertThat(MDC.get(tenantKey)).isEqualTo(tenantValue);

        listener.onClosed(new TenantContextClosedEvent(tenantValue, this));

        assertThat(MDC.get(tenantKey)).isNull();
    }

    @Test
    void whenCustomValueIsUsedAsKey() {
        var tenantKey = "tenant_id";
        var tenantValue = "acme";
        var listener = new MdcTenantEventListener(tenantKey);

        listener.onAttached(new TenantContextAttachedEvent(tenantValue, this));

        assertThat(MDC.get(tenantKey)).isEqualTo(tenantValue);

        listener.onClosed(new TenantContextClosedEvent(tenantValue, this));

        assertThat(MDC.get(tenantKey)).isNull();
    }

}
