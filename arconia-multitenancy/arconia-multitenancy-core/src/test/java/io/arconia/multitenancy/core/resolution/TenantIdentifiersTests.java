package io.arconia.multitenancy.core.resolution;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TenantIdentifiers}.
 */
class TenantIdentifiersTests {

    @Test
    void whenPlainValueThenUnchanged() {
        assertThat(TenantIdentifiers.sanitizeForLogging("acme")).isEqualTo("acme");
    }

    @Test
    void whenControlCharactersThenReplaced() {
        assertThat(TenantIdentifiers.sanitizeForLogging("acme\nfake log line\r\t"))
            .isEqualTo("acme�fake log line��");
    }

    @Test
    void whenLongerThanLimitThenTruncatedWithEllipsis() {
        var sanitized = TenantIdentifiers.sanitizeForLogging("a".repeat(100));

        assertThat(sanitized).hasSize(65).startsWith("a".repeat(64)).endsWith("…");
    }

    @Test
    void whenAtLimitThenUnchanged() {
        var value = "a".repeat(64);

        assertThat(TenantIdentifiers.sanitizeForLogging(value)).isEqualTo(value);
    }

}
