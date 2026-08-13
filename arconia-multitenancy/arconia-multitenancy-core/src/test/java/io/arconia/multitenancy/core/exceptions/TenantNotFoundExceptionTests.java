package io.arconia.multitenancy.core.exceptions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TenantNotFoundException}.
 */
class TenantNotFoundExceptionTests {

    @Test
    void whenDefaultMessage() {
        var exception = new TenantNotFoundException();
        assertThat(exception).hasMessageContaining("No tenant found in the current context");
    }

    @Test
    void whenCustomMessage() {
        var message = "Custom tenant exception message";
        var exception = new TenantNotFoundException(message);
        assertThat(exception).hasMessageContaining(message);
    }

    @Test
    void whenCauseThenCauseIsRetained() {
        var cause = new IllegalArgumentException("root cause");
        var exception = new TenantNotFoundException("Custom tenant exception message", cause);

        assertThat(exception).hasMessageContaining("Custom tenant exception message").hasCause(cause);
    }

}
