package io.arconia.data.jdbc.sqlite.dialect;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link NumberToBooleanConverter}.
 */
class NumberToBooleanConverterTests {

    @ParameterizedTest
    @MethodSource("zeros")
    void zeroIsFalse(Number zero) {
        assertThat(NumberToBooleanConverter.INSTANCE.convert(zero)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("nonZeros")
    void nonZeroIsTrue(Number nonZero) {
        assertThat(NumberToBooleanConverter.INSTANCE.convert(nonZero)).isTrue();
    }

    static Stream<Number> zeros() {
        return Stream.of((byte) 0, (short) 0, 0, 0L, 0.0f, 0.0d, -0.0d, BigDecimal.ZERO);
    }

    static Stream<Number> nonZeros() {
        return Stream.of((byte) 1, (short) 1, 1, -1, Integer.MIN_VALUE, 1L, 1L << 32, Long.MAX_VALUE,
                0.5f, 0.5d, BigDecimal.ONE);
    }

}
