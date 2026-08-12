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

}
