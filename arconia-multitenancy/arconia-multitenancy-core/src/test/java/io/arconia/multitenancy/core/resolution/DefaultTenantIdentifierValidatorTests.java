package io.arconia.multitenancy.core.resolution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
        assertThatThrownBy(() -> validator.validate(null)).isInstanceOf(InvalidTenantIdentifierException.class)
            .hasMessageContaining("The tenant identifier must not be empty");
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "   " })
    void whenBlankThenThrow(String tenantIdentifier) {
        assertThatThrownBy(() -> validator.validate(tenantIdentifier)).isInstanceOf(InvalidTenantIdentifierException.class)
            .hasMessageContaining("The tenant identifier must not be empty");
    }

    @ParameterizedTest
    @ValueSource(strings = { "acme\nmalicious", "../../etc/passwd", "acme corp", "acme;drop", "acme?x=1", "acmé" })
    void whenInvalidCharactersThenThrow(String tenantIdentifier) {
        assertThatThrownBy(() -> validator.validate(tenantIdentifier)).isInstanceOf(InvalidTenantIdentifierException.class)
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
            .isInstanceOf(InvalidTenantIdentifierException.class)
            .hasMessageContaining("The tenant identifier must not be longer than 64 characters");
    }

    @Test
    void whenCustomMaximumLengthThenApplied() {
        var customValidator = DefaultTenantIdentifierValidator.builder().maxLength(4).build();

        assertThatNoException().isThrownBy(() -> customValidator.validate("acme"));
        assertThatThrownBy(() -> customValidator.validate("acme1")).isInstanceOf(InvalidTenantIdentifierException.class)
            .hasMessageContaining("The tenant identifier must not be longer than 4 characters");
    }

    @Test
    void whenMaximumLengthIsNotPositiveThenThrow() {
        assertThatThrownBy(() -> DefaultTenantIdentifierValidator.builder().maxLength(0).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("maxLength must be greater than zero");
    }

    @Test
    void whenDefaultsThenPatternAcceptsAlphanumericDashesAndUnderscores() {
        assertThat(validator.getPattern()).isEqualTo("[a-zA-Z0-9_-]+");
    }

    @Test
    void whenCustomPatternThenApplied() {
        var customValidator = DefaultTenantIdentifierValidator.builder().pattern("[a-z]+(\\.[a-z]+)*").build();

        assertThatNoException().isThrownBy(() -> customValidator.validate("acme.corp"));
        assertThatThrownBy(() -> customValidator.validate("ACME")).isInstanceOf(InvalidTenantIdentifierException.class)
            .hasMessageContaining("The tenant identifier must match the pattern");
    }

    @Test
    void whenCustomPatternThenMatchedInFull() {
        var customValidator = DefaultTenantIdentifierValidator.builder().pattern("[a-z]+").build();

        assertThatThrownBy(() -> customValidator.validate("acme1")).isInstanceOf(InvalidTenantIdentifierException.class);
    }

    @Test
    void whenPatternIsMalformedThenFailFast() {
        assertThatThrownBy(() -> DefaultTenantIdentifierValidator.builder().pattern("[a-z").build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("pattern is not a valid regular expression");
    }

    @Test
    void whenPatternIsEmptyThenThrow() {
        assertThatThrownBy(() -> DefaultTenantIdentifierValidator.builder().pattern("").build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("pattern cannot be null or empty");
    }

}
