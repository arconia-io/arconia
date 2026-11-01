package io.arconia.data.jdbc.sqlite.dialect;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

/**
 * Converters storing {@code java.time} values as ISO-8601 text, the first of the date and time
 * formats recommended by SQLite, which has no dedicated storage class for them.
 * <p>
 * Values are written in the format of SQLite's own date and time functions, such as
 * {@code datetime()} and {@code CURRENT_TIMESTAMP}: a space between date and time and no offset,
 * since SQLite treats every date and time as UTC. SQLite compares them as text, so sharing that
 * format keeps ordering and range comparisons correct against values SQLite produces. The
 * fraction has nine digits, so values round-trip without losing precision. {@link Instant} and
 * {@link OffsetDateTime} values are normalized to UTC.
 * <p>
 * Values are read leniently: the separator between date and time can be {@code T} or a space,
 * the fraction can have any number of digits, and a missing offset means UTC.
 *
 * @see <a href="https://www.sqlite.org/datatype3.html#date_and_time_datatype">SQLite Date and Time Datatype</a>
 * @see <a href="https://www.sqlite.org/lang_datefunc.html">SQLite Date and Time Functions</a>
 */
final class TemporalConverters {

    private static final DateTimeFormatter TIME_WRITER = DateTimeFormatter.ofPattern("HH:mm:ss.SSSSSSSSS");

    private static final DateTimeFormatter LOCAL_DATE_TIME_WRITER = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral(' ')
            .append(TIME_WRITER)
            .toFormatter();

    private static final DateTimeFormatter UTC_DATE_TIME_WRITER = LOCAL_DATE_TIME_WRITER.withZone(ZoneOffset.UTC);

    private static final DateTimeFormatter LOCAL_DATE_TIME_READER = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .optionalStart().appendLiteral('T').optionalEnd()
            .optionalStart().appendLiteral(' ').optionalEnd()
            .append(DateTimeFormatter.ISO_LOCAL_TIME)
            .toFormatter();

    private static final DateTimeFormatter OFFSET_DATE_TIME_READER = new DateTimeFormatterBuilder()
            .append(LOCAL_DATE_TIME_READER)
            .optionalStart().appendOffsetId().optionalEnd()
            .parseDefaulting(ChronoField.OFFSET_SECONDS, 0)
            .toFormatter();

    private TemporalConverters() {}

    static List<Object> converters() {
        return List.of(
                InstantToStringConverter.INSTANCE,
                StringToInstantConverter.INSTANCE,
                OffsetDateTimeToStringConverter.INSTANCE,
                StringToOffsetDateTimeConverter.INSTANCE,
                LocalDateTimeToStringConverter.INSTANCE,
                StringToLocalDateTimeConverter.INSTANCE,
                LocalDateToStringConverter.INSTANCE,
                StringToLocalDateConverter.INSTANCE,
                LocalTimeToStringConverter.INSTANCE,
                StringToLocalTimeConverter.INSTANCE
        );
    }

    @WritingConverter
    enum InstantToStringConverter implements Converter<Instant, String> {

        INSTANCE;

        @Override
        public String convert(Instant source) {
            return UTC_DATE_TIME_WRITER.format(source);
        }

    }

    @ReadingConverter
    enum StringToInstantConverter implements Converter<String, Instant> {

        INSTANCE;

        @Override
        public Instant convert(String source) {
            return OffsetDateTime.parse(source, OFFSET_DATE_TIME_READER).toInstant();
        }

    }

    @WritingConverter
    enum OffsetDateTimeToStringConverter implements Converter<OffsetDateTime, String> {

        INSTANCE;

        @Override
        public String convert(OffsetDateTime source) {
            return UTC_DATE_TIME_WRITER.format(source);
        }

    }

    @ReadingConverter
    enum StringToOffsetDateTimeConverter implements Converter<String, OffsetDateTime> {

        INSTANCE;

        @Override
        public OffsetDateTime convert(String source) {
            return OffsetDateTime.parse(source, OFFSET_DATE_TIME_READER);
        }

    }

    @WritingConverter
    enum LocalDateTimeToStringConverter implements Converter<LocalDateTime, String> {

        INSTANCE;

        @Override
        public String convert(LocalDateTime source) {
            return LOCAL_DATE_TIME_WRITER.format(source);
        }

    }

    @ReadingConverter
    enum StringToLocalDateTimeConverter implements Converter<String, LocalDateTime> {

        INSTANCE;

        @Override
        public LocalDateTime convert(String source) {
            return LocalDateTime.parse(source, LOCAL_DATE_TIME_READER);
        }

    }

    @WritingConverter
    enum LocalDateToStringConverter implements Converter<LocalDate, String> {

        INSTANCE;

        @Override
        public String convert(LocalDate source) {
            return DateTimeFormatter.ISO_LOCAL_DATE.format(source);
        }

    }

    @ReadingConverter
    enum StringToLocalDateConverter implements Converter<String, LocalDate> {

        INSTANCE;

        @Override
        public LocalDate convert(String source) {
            return LocalDate.parse(source, DateTimeFormatter.ISO_LOCAL_DATE);
        }

    }

    @WritingConverter
    enum LocalTimeToStringConverter implements Converter<LocalTime, String> {

        INSTANCE;

        @Override
        public String convert(LocalTime source) {
            return TIME_WRITER.format(source);
        }

    }

    @ReadingConverter
    enum StringToLocalTimeConverter implements Converter<String, LocalTime> {

        INSTANCE;

        @Override
        public LocalTime convert(String source) {
            return LocalTime.parse(source, DateTimeFormatter.ISO_LOCAL_TIME);
        }

    }

}
