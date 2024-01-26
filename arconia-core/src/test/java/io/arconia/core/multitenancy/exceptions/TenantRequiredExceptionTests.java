package io.arconia.core.multitenancy.exceptions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantRequiredExceptionTests {

    @Test
    void whenDefaultMessage() {
        var exception = new TenantRequiredException();
        assertThat(exception).hasMessageContaining("A tenant must be specified for the current operation");
    }

    @Test
    void whenCustomMessage() {
        var message = "Custom tenant exception message";
        var exception = new TenantRequiredException(message);
        assertThat(exception).hasMessageContaining(message);
    }

}
