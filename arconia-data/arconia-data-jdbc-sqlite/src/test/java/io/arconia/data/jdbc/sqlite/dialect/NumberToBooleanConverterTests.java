package io.arconia.data.jdbc.sqlite.dialect;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link NumberToBooleanConverter}.
 */
class NumberToBooleanConverterTests {

    @Test
    void zeroIsFalse() {
        assertThat(NumberToBooleanConverter.INSTANCE.convert(0)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, -1, 2, 42, Integer.MAX_VALUE, Integer.MIN_VALUE })
    void nonZeroIsTrue(int value) {
        assertThat(NumberToBooleanConverter.INSTANCE.convert(value)).isTrue();
    }

    @Test
    void supportsAllNumberSubtypes() {
        assertThat(NumberToBooleanConverter.INSTANCE.convert((byte) 0)).isFalse();
        assertThat(NumberToBooleanConverter.INSTANCE.convert((byte) 1)).isTrue();
        assertThat(NumberToBooleanConverter.INSTANCE.convert((short) 0)).isFalse();
        assertThat(NumberToBooleanConverter.INSTANCE.convert((short) 1)).isTrue();
        assertThat(NumberToBooleanConverter.INSTANCE.convert(0L)).isFalse();
        assertThat(NumberToBooleanConverter.INSTANCE.convert(1L)).isTrue();
        assertThat(NumberToBooleanConverter.INSTANCE.convert(0.0f)).isFalse();
        assertThat(NumberToBooleanConverter.INSTANCE.convert(1.0f)).isTrue();
        assertThat(NumberToBooleanConverter.INSTANCE.convert(0.0d)).isFalse();
        assertThat(NumberToBooleanConverter.INSTANCE.convert(1.0d)).isTrue();
        assertThat(NumberToBooleanConverter.INSTANCE.convert(BigDecimal.ZERO)).isFalse();
        assertThat(NumberToBooleanConverter.INSTANCE.convert(BigDecimal.ONE)).isTrue();
    }

    @Test
    void fractionalValueBelowOneIsFalse() {
        assertThat(NumberToBooleanConverter.INSTANCE.convert(0.5d)).isFalse();
    }

    @Test
    void nullThrowsException() {
        assertThatThrownBy(() -> NumberToBooleanConverter.INSTANCE.convert(null))
                .isInstanceOf(NullPointerException.class);
    }

}
