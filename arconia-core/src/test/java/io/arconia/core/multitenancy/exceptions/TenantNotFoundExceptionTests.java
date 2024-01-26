package io.arconia.core.multitenancy.exceptions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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

}
