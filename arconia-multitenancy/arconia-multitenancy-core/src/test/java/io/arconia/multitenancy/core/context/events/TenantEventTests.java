package io.arconia.multitenancy.core.context.events;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TenantEvent}.
 */
class TenantEventTests {

    @Test
    void whenNullTenantIdentifierThenThrow() {
        assertThatThrownBy(() -> new TenantContextAttachedEvent(null, this))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantIdentifier cannot be null or empty");
    }

    @Test
    void whenEmptyTenantIdentifierThenThrow() {
        assertThatThrownBy(() -> new TenantContextClosedEvent("", this)).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantIdentifier cannot be null or empty");
    }

    @Test
    void whenNullSourceThenThrow() {
        assertThatThrownBy(() -> new TenantContextAttachedEvent("acme", null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void whenAttachedThenCarriesTenantIdentifierAndSource() {
        var event = new TenantContextAttachedEvent("acme", this);

        assertThat(event.getTenantIdentifier()).isEqualTo("acme");
        assertThat(event.getSource()).isSameAs(this);
    }

    @Test
    void whenClosedThenCarriesTenantIdentifierAndSource() {
        var event = new TenantContextClosedEvent("acme", this);

        assertThat(event.getTenantIdentifier()).isEqualTo("acme");
        assertThat(event.getSource()).isSameAs(this);
    }

    @Test
    void whenSealedThenOnlyFrameworkEventsArePermitted() {
        assertThat(TenantEvent.class.isSealed()).isTrue();
        assertThat(TenantEvent.class.getPermittedSubclasses()).containsExactlyInAnyOrder(
                TenantContextAttachedEvent.class, TenantContextClosedEvent.class);
    }

}
