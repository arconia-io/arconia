package io.arconia.data.jdbc.sqlite.dialect;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

/**
 * Reads a {@link Number} as a {@link Boolean}: zero maps to {@code false}, any other value
 * maps to {@code true}.
 *
 * <p>Based on {@code org.springframework.data.relational.core.dialect.NumberToBooleanConverter}
 * (Spring Data Relational, Apache-2.0).
 */
@ReadingConverter
enum NumberToBooleanConverter implements Converter<Number, Boolean> {

    INSTANCE;

    @Override
    public Boolean convert(Number number) {
        return number.intValue() != 0;
    }

}
