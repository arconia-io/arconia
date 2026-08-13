package io.arconia.multitenancy.core.exceptions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TenantVerificationException}.
 */
class TenantVerificationExceptionTests {

    @Test
    void whenDefaultMessage() {
        var exception = new TenantVerificationException();
        assertThat(exception).hasMessageContaining("Tenant verification failed");
    }

    @Test
    void whenCustomMessage() {
        var message = "Custom tenant exception message";
        var exception = new TenantVerificationException(message);
        assertThat(exception).hasMessageContaining(message);
    }

    @Test
    void whenCauseThenCauseIsRetained() {
        var cause = new IllegalStateException("root cause");
        var exception = new TenantVerificationException("Custom tenant exception message", cause);

        assertThat(exception).hasMessageContaining("Custom tenant exception message").hasCause(cause);
    }

}
