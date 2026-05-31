package io.arconia.data.jdbc.sqlite.dialect;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

/**
 * A {@link ReadingConverter} to convert from {@link Number} to {@link Boolean}.
 * 0 is considered {@literal false}, everything else is considered {@literal true}.
 * <p>
 * Based on org.springframework.data.relational.core.dialect.NumberToBooleanConverter
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
