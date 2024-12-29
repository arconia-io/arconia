package io.arconia.multitenancy.core.exceptions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TenantResolutionException}.
 */
class TenantResolutionExceptionTests {

    @Test
    void whenDefaultMessage() {
        var exception = new TenantResolutionException();
        assertThat(exception).hasMessageContaining("Error when trying to resolve the current tenant");
    }

    @Test
    void whenCustomMessage() {
        var message = "Custom tenant exception message";
        var exception = new TenantResolutionException(message);
        assertThat(exception).hasMessageContaining(message);
    }

}
