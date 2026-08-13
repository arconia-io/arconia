package io.arconia.multitenancy.core.tenantdetails;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.arconia.multitenancy.core.exceptions.TenantVerificationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DefaultTenantIdentifierValidator}.
 */
class DefaultTenantIdentifierValidatorTests {

    private final DefaultTenantIdentifierValidator validator = DefaultTenantIdentifierValidator.builder().build();

    @ParameterizedTest
    @ValueSource(strings = { "acme", "acme-corp_2", "ACME", "0", "a-b_c-1" })
    void whenValidIdentifierThenPass(String tenantIdentifier) {
        assertThatNoException().isThrownBy(() -> validator.validate(tenantIdentifier));
    }

    @Test
    void whenNullThenThrow() {
        assertThatThrownBy(() -> validator.validate(null)).isInstanceOf(TenantVerificationException.class)
            .hasMessageContaining("The tenant identifier must not be empty");
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "   " })
    void whenBlankThenThrow(String tenantIdentifier) {
        assertThatThrownBy(() -> validator.validate(tenantIdentifier)).isInstanceOf(TenantVerificationException.class)
            .hasMessageContaining("The tenant identifier must not be empty");
    }

    @ParameterizedTest
    @ValueSource(strings = { "acme\nmalicious", "../../etc/passwd", "acme corp", "acme;drop", "acme?x=1", "acmé" })
    void whenInvalidCharactersThenThrow(String tenantIdentifier) {
        assertThatThrownBy(() -> validator.validate(tenantIdentifier)).isInstanceOf(TenantVerificationException.class)
            .hasMessageContaining("The tenant identifier must contain only alphanumeric characters");
    }

    @Test
    void whenDefaultsThenMaximumLengthIs64() {
        assertThat(validator.getMaxLength()).isEqualTo(64);
    }

    @Test
    void whenAtMaximumLengthThenPass() {
        assertThatNoException().isThrownBy(() -> validator.validate("a".repeat(validator.getMaxLength())));
    }

    @Test
    void whenLongerThanMaximumLengthThenThrow() {
        assertThatThrownBy(() -> validator.validate("a".repeat(validator.getMaxLength() + 1)))
            .isInstanceOf(TenantVerificationException.class)
            .hasMessageContaining("The tenant identifier must not be longer than 64 characters");
    }

    @Test
    void whenCustomMaximumLengthThenApplied() {
        var customValidator = DefaultTenantIdentifierValidator.builder().maxLength(4).build();

        assertThatNoException().isThrownBy(() -> customValidator.validate("acme"));
        assertThatThrownBy(() -> customValidator.validate("acme1")).isInstanceOf(TenantVerificationException.class)
            .hasMessageContaining("The tenant identifier must not be longer than 4 characters");
    }

    @Test
    void whenMaximumLengthIsNotPositiveThenThrow() {
        assertThatThrownBy(() -> DefaultTenantIdentifierValidator.builder().maxLength(0).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("maxLength must be greater than zero");
    }

}
