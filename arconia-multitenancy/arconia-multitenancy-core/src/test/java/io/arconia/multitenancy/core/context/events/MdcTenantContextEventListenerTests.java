package io.arconia.multitenancy.core.context.events;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link MdcTenantContextEventListener}.
 */
class MdcTenantContextEventListenerTests {

    @Test
    void whenNullCustomValueThenThrow() {
        assertThatThrownBy(() -> new MdcTenantContextEventListener(null)).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantIdentifierKey cannot be null or empty");
    }

    @Test
    void whenEmptyCustomValueThenThrow() {
        assertThatThrownBy(() -> new MdcTenantContextEventListener("")).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantIdentifierKey cannot be null or empty");
    }

    @Test
    void whenDefaultValueIsUsedAsKey() {
        var tenantKey = "tenantId";
        var tenantValue = "acme";
        var listener = new MdcTenantContextEventListener();

        listener.onApplicationEvent(new TenantContextAttachedEvent(tenantValue, this));

        assertThat(MDC.get(tenantKey)).isEqualTo(tenantValue);

        listener.onApplicationEvent(new TenantContextClosedEvent(tenantValue, this));

        assertThat(MDC.get(tenantKey)).isNull();
    }

    @Test
    void whenCustomValueIsUsedAsKey() {
        var tenantKey = "tenant_id";
        var tenantValue = "acme";
        var listener = new MdcTenantContextEventListener(tenantKey);

        listener.onApplicationEvent(new TenantContextAttachedEvent(tenantValue, this));

        assertThat(MDC.get(tenantKey)).isEqualTo(tenantValue);

        listener.onApplicationEvent(new TenantContextClosedEvent(tenantValue, this));

        assertThat(MDC.get(tenantKey)).isNull();
    }

}
