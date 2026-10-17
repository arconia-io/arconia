package io.arconia.multitenancy.core.context;

import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TenantContext}.
 */
class TenantContextTests {

    @Test
    void whenBoundThenGetTenantIdentifierReturnsValue() {
        TenantContext.where("acme").run(() ->
            assertThat(TenantContext.getTenantIdentifier()).isEqualTo("acme")
        );
    }

    @Test
    void whenNotBoundThenGetTenantIdentifierReturnsNull() {
        assertThat(TenantContext.getTenantIdentifier()).isNull();
    }

    @Test
    void whenBoundThenGetRequiredTenantIdentifierReturnsValue() {
        TenantContext.where("acme").run(() ->
            assertThat(TenantContext.getRequiredTenantIdentifier()).isEqualTo("acme")
        );
    }

    @Test
    void whenNotBoundThenGetRequiredTenantIdentifierThrows() {
        assertThatThrownBy(TenantContext::getRequiredTenantIdentifier).isInstanceOf(TenantNotFoundException.class)
            .hasMessageContaining("No tenant found in the current context");
    }

    @Test
    void whenScopeExitsThenTenantIsUnbound() {
        TenantContext.where("acme").run(() ->
            assertThat(TenantContext.getTenantIdentifier()).isEqualTo("acme")
        );
        assertThat(TenantContext.getTenantIdentifier()).isNull();
    }

    @Test
    void whenNullTenantIdentifierThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> TenantContext.where(null))
                .withMessageContaining("tenantIdentifier cannot be null or empty");
    }

    @Test
    void whenEmptyTenantIdentifierThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> TenantContext.where(""))
                .withMessageContaining("tenantIdentifier cannot be null or empty");
    }

    @Test
    void whenBlankTenantIdentifierThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> TenantContext.where("  "))
                .withMessageContaining("tenantIdentifier cannot be null or empty");
    }

    @Test
    void whenNestedScopesThenInnerBindingShadowsOuter() {
        TenantContext.where("acme").run(() -> {
            assertThat(TenantContext.getTenantIdentifier()).isEqualTo("acme");
            TenantContext.where("beans").run(() ->
                assertThat(TenantContext.getTenantIdentifier()).isEqualTo("beans")
            );
            assertThat(TenantContext.getTenantIdentifier()).isEqualTo("acme");
        });
    }

}
