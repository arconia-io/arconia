package io.arconia.core.multitenancy.events;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TenantEvent}.
 *
 * @author Thomas Vitale
 */
class TenantEventTests {

    @Test
    void whenNullTenantIdThenThrow() {
        assertThatThrownBy(() -> new TestTenantEvent(null, this)).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantId cannot be empty");
    }

    @Test
    void whenEmptyTenantIdThenThrow() {
        assertThatThrownBy(() -> new TestTenantEvent("", this)).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantId cannot be empty");
    }

    @Test
    void whenNullSourceThenThrow() {
        assertThatThrownBy(() -> new TestTenantEvent("tenant", null)).isInstanceOf(IllegalArgumentException.class);
    }

    static class TestTenantEvent extends TenantEvent {

        public TestTenantEvent(String tenantId, Object source) {
            super(tenantId, source);
        }

    }

}
