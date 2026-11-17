package io.arconia.data.jdbc.sqlite.dialect;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.InstantToStringConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.LocalDateToStringConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.LocalTimeToStringConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.StringToInstantConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.StringToLocalDateConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.StringToLocalTimeConverter;

/**
 * Converters storing the {@code java.sql} date and time types in the same text formats as
 * {@link TemporalConverters}.
 * <p>
 * A {@link Timestamp} is stored as the instant it represents, like an {@link java.time.Instant}.
 * A {@link Date} and a {@link Time} are stored like a {@link java.time.LocalDate} and a
 * {@link java.time.LocalTime}, converted in the default time zone of the JVM as their
 * {@code toLocalDate()} and {@code toLocalTime()} methods do. A {@link Time} keeps whole seconds
 * only.
 */
final class SqlTemporalConverters {

    private SqlTemporalConverters() {}

    static List<Object> converters() {
        return List.of(
                TimestampToStringConverter.INSTANCE,
                StringToTimestampConverter.INSTANCE,
                DateToStringConverter.INSTANCE,
                StringToDateConverter.INSTANCE,
                TimeToStringConverter.INSTANCE,
                StringToTimeConverter.INSTANCE
        );
    }

    @WritingConverter
    enum TimestampToStringConverter implements Converter<Timestamp, String> {

        INSTANCE;

        @Override
        public String convert(Timestamp source) {
            return InstantToStringConverter.INSTANCE.convert(source.toInstant());
        }

    }

    @ReadingConverter
    enum StringToTimestampConverter implements Converter<String, Timestamp> {

        INSTANCE;

        @Override
        public Timestamp convert(String source) {
            return Timestamp.from(StringToInstantConverter.INSTANCE.convert(source));
        }

    }

    @WritingConverter
    enum DateToStringConverter implements Converter<Date, String> {

        INSTANCE;

        @Override
        public String convert(Date source) {
            return LocalDateToStringConverter.INSTANCE.convert(source.toLocalDate());
        }

    }

    @ReadingConverter
    enum StringToDateConverter implements Converter<String, Date> {

        INSTANCE;

        @Override
        public Date convert(String source) {
            return Date.valueOf(StringToLocalDateConverter.INSTANCE.convert(source));
        }

    }

    @WritingConverter
    enum TimeToStringConverter implements Converter<Time, String> {

        INSTANCE;

        @Override
        public String convert(Time source) {
            return LocalTimeToStringConverter.INSTANCE.convert(source.toLocalTime());
        }

    }

    @ReadingConverter
    enum StringToTimeConverter implements Converter<String, Time> {

        INSTANCE;

        @Override
        public Time convert(String source) {
            return Time.valueOf(StringToLocalTimeConverter.INSTANCE.convert(source));
        }

    }

}
