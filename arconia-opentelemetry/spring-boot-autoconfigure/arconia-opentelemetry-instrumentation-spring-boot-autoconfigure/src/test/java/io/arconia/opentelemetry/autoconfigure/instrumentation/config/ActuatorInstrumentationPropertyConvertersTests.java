package io.arconia.opentelemetry.autoconfigure.instrumentation.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ActuatorInstrumentationPropertyConverters}.
 */
class ActuatorInstrumentationPropertyConvertersTests {

    @ParameterizedTest
    @CsvSource({
            "nanoseconds, NANOSECONDS",
            "microseconds, MICROSECONDS",
            "milliseconds, MILLISECONDS",
            "seconds, SECONDS",
            "minutes, MINUTES",
            "hours, HOURS",
            "days, DAYS",
            "NANOSECONDS, NANOSECONDS",
            "MICROSECONDS, MICROSECONDS",
            "MILLISECONDS, MILLISECONDS",
            "SECONDS, SECONDS",
            "MINUTES, MINUTES",
            "HOURS, HOURS",
            "DAYS, DAYS",
            "' seconds ', SECONDS",
            "'\tminutes\n', MINUTES"
    })
    void baseTimeUnitShouldConvertValidValues(String input, TimeUnit expected) {
        Function<String, TimeUnit> converter = ActuatorInstrumentationPropertyConverters.baseTimeUnit("test.key");
        assertThat(converter.apply(input)).isEqualTo(expected);
    }

    @Test
    void baseTimeUnitShouldReturnNullForInvalidValue() {
        Function<String, TimeUnit> converter = ActuatorInstrumentationPropertyConverters.baseTimeUnit("test.key");
        assertThat(converter.apply("invalid")).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void baseTimeUnitShouldThrowExceptionForInvalidKey(String key) {
        assertThatThrownBy(() -> ActuatorInstrumentationPropertyConverters.baseTimeUnit(key))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("externalKey cannot be null or empty");
    }

}
