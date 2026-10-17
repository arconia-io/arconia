package io.arconia.multitenancy.core.observability;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import io.arconia.multitenancy.core.context.TenantContextAttachedEvent;
import io.arconia.multitenancy.core.context.TenantContextClosedEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link MdcTenantEventListener}.
 */
class MdcTenantEventListenerTests {

    @Test
    void whenNullCustomValueThenThrow() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> MdcTenantEventListener.builder().tenantIdentifierKey(null).build())
            .withMessageContaining("tenantIdentifierKey cannot be null or empty");
    }

    @Test
    void whenEmptyCustomValueThenThrow() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> MdcTenantEventListener.builder().tenantIdentifierKey("").build())
            .withMessageContaining("tenantIdentifierKey cannot be null or empty");
    }

    @Test
    void whenDefaultsThenTenantIdKey() {
        assertThat(MdcTenantEventListener.builder().build().getTenantIdentifierKey()).isEqualTo("tenantId");
    }

    @Test
    void whenDefaultValueIsUsedAsKey() {
        var tenantValue = "acme";
        var listener = MdcTenantEventListener.builder().build();
        var tenantKey = listener.getTenantIdentifierKey();

        listener.onAttached(new TenantContextAttachedEvent(tenantValue, this));

        assertThat(MDC.get(tenantKey)).isEqualTo(tenantValue);

        listener.onClosed(new TenantContextClosedEvent(tenantValue, this));

        assertThat(MDC.get(tenantKey)).isNull();
    }

    @Test
    void whenCustomValueIsUsedAsKey() {
        var tenantKey = "tenant_id";
        var tenantValue = "acme";
        var listener = MdcTenantEventListener.builder().tenantIdentifierKey(tenantKey).build();

        listener.onAttached(new TenantContextAttachedEvent(tenantValue, this));

        assertThat(MDC.get(tenantKey)).isEqualTo(tenantValue);

        listener.onClosed(new TenantContextClosedEvent(tenantValue, this));

        assertThat(MDC.get(tenantKey)).isNull();
    }

}
