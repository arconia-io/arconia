package io.arconia.core.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link BootstrapMode}.
 */
class BootstrapModeTests {

    @ParameterizedTest
    @CsvSource({ "DEV, DEV", "test, TEST", "Prod, PROD", "'\n dev', DEV", "'prod   \n', PROD" })
    void shouldParseValidMode(String value, BootstrapMode expected) {
        assertThat(BootstrapMode.parse(value)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   ", "INVALID", "development" })
    void shouldNotParseInvalidMode(String value) {
        assertThat(BootstrapMode.parse(value)).isNull();
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "dev, test, dev",
            "dev, null, dev",
            "null, test, test",
            "'  ', test, test",
            "null, '', null",
            "null, null, null"
    })
    void systemPropertyTakesPrecedenceOverEnvironmentVariable(String systemProperty, String environmentVariable, String expected) {
        assertThat(BootstrapMode.getExplicitValue(systemProperty, environmentVariable)).isEqualTo(expected);
    }

    @Test
    void environmentVariableMatchesPropertyKey() {
        assertThat(BootstrapMode.ENVIRONMENT_VARIABLE).isEqualTo(BootstrapMode.PROPERTY_KEY.toUpperCase().replace('.', '_'));
    }

}
