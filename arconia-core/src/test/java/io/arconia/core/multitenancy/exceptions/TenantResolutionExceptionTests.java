package io.arconia.core.multitenancy.exceptions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TenantResolutionException}.
 *
 * @author Thomas Vitale
 */
class TenantResolutionExceptionTests {

    @Test
    void whenDefaultMessage() {
        var exception = new TenantResolutionException();
        assertThat(exception).hasMessageContaining("A tenant must be specified for the current operation");
    }

    @Test
    void whenCustomMessage() {
        var message = "Custom tenant exception message";
        var exception = new TenantResolutionException(message);
        assertThat(exception).hasMessageContaining(message);
    }

}
