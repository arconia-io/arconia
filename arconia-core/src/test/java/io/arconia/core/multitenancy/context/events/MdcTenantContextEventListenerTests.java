package io.arconia.core.multitenancy.context.events;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link MdcTenantContextEventListener}.
 *
 * @author Thomas Vitale
 */
class MdcTenantContextEventListenerTests {

    @Test
    void whenNullCustomValueThenThrow() {
        assertThatThrownBy(() -> new MdcTenantContextEventListener(null)).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantIdKey cannot be empty");
    }

    @Test
    void whenEmptyCustomValueThenThrow() {
        assertThatThrownBy(() -> new MdcTenantContextEventListener("")).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantIdKey cannot be empty");
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
