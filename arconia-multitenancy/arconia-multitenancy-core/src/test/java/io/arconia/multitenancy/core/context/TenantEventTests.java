package io.arconia.multitenancy.core.context;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link TenantEvent}.
 */
class TenantEventTests {

    @Test
    void whenNullTenantIdentifierThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new TenantContextAttachedEvent(null, this))
                .withMessageContaining("tenantIdentifier cannot be null or empty");
    }

    @Test
    void whenEmptyTenantIdentifierThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new TenantContextClosedEvent("", this))
                .withMessageContaining("tenantIdentifier cannot be null or empty");
    }

    @Test
    void whenNullSourceThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new TenantContextAttachedEvent("acme", null))
                .withMessageContaining("null source");
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

}
