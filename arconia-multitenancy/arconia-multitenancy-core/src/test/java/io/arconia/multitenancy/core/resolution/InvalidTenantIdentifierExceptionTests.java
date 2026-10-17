package io.arconia.multitenancy.core.resolution;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link InvalidTenantIdentifierException}.
 */
class InvalidTenantIdentifierExceptionTests {

    @Test
    void whenCustomMessage() {
        var exception = new InvalidTenantIdentifierException("The tenant identifier must not be empty");

        assertThat(exception).hasMessageContaining("The tenant identifier must not be empty")
            .isInstanceOf(TenantVerificationException.class);
    }

    @Test
    void whenCauseThenCauseIsRetained() {
        var cause = new IllegalStateException("root cause");
        var exception = new InvalidTenantIdentifierException("Invalid tenant identifier", cause);

        assertThat(exception).hasMessageContaining("Invalid tenant identifier").hasCause(cause);
    }

}
