package io.arconia.data.jdbc.sqlite.dialect;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Date;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.DateToStringConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.InstantToStringConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.LocalDateTimeToStringConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.LocalDateToStringConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.LocalTimeToStringConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.NumberToYearConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.OffsetDateTimeToStringConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.OffsetTimeToStringConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.StringToDateConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.StringToInstantConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.StringToLocalDateConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.StringToLocalDateTimeConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.StringToLocalTimeConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.StringToOffsetDateTimeConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.StringToOffsetTimeConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.StringToYearConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.StringToYearMonthConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.StringToZonedDateTimeConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.YearMonthToStringConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.YearToIntegerConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.ZonedDateTimeToStringConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TemporalConverters}.
 */
class TemporalConvertersTests {

    @Nested
    class Writing {

        @Test
        void shouldWriteInstantAsUtcText() {
            assertThat(InstantToStringConverter.INSTANCE.convert(Instant.parse("2026-07-31T10:34:56Z")))
                    .isEqualTo("2026-07-31 10:34:56.000000000");
        }

        @Test
        void shouldWriteOffsetDateTimeNormalizedToUtc() {
            OffsetDateTime source = OffsetDateTime.of(2026, 7, 31, 12, 34, 56, 123_000_000, ZoneOffset.ofHours(2));

            assertThat(OffsetDateTimeToStringConverter.INSTANCE.convert(source))
                    .isEqualTo("2026-07-31 10:34:56.123000000");
        }

        @Test
        void shouldWriteLocalDateTimeAsText() {
            assertThat(LocalDateTimeToStringConverter.INSTANCE.convert(LocalDateTime.of(2026, 7, 31, 12, 34)))
                    .isEqualTo("2026-07-31 12:34:00.000000000");
        }

        @Test
        void shouldWriteLocalDateAsText() {
            assertThat(LocalDateToStringConverter.INSTANCE.convert(LocalDate.of(2026, 7, 31)))
                    .isEqualTo("2026-07-31");
        }

        @Test
        void shouldWriteLocalTimeAsText() {
            assertThat(LocalTimeToStringConverter.INSTANCE.convert(LocalTime.of(12, 34)))
                    .isEqualTo("12:34:00.000000000");
        }

        @Test
        void shouldWriteZonedDateTimeNormalizedToUtc() {
            ZonedDateTime source = ZonedDateTime.of(2026, 7, 31, 12, 34, 56, 0, ZoneId.of("Europe/Rome"));

            assertThat(ZonedDateTimeToStringConverter.INSTANCE.convert(source))
                    .isEqualTo("2026-07-31 10:34:56.000000000");
        }

        @Test
        void shouldWriteOffsetTimeNormalizedToUtc() {
            assertThat(OffsetTimeToStringConverter.INSTANCE.convert(OffsetTime.of(12, 34, 0, 0, ZoneOffset.ofHours(2))))
                    .isEqualTo("10:34:00.000000000");
        }

        @Test
        void shouldWriteYearAsInteger() {
            assertThat(YearToIntegerConverter.INSTANCE.convert(Year.of(2026))).isEqualTo(2026);
        }

        @Test
        void shouldWriteYearMonthAsText() {
            assertThat(YearMonthToStringConverter.INSTANCE.convert(YearMonth.of(2026, 7))).isEqualTo("2026-07");
        }

        @Test
        void shouldWriteDateAsUtcText() {
            assertThat(DateToStringConverter.INSTANCE.convert(Date.from(Instant.parse("2026-07-31T10:34:56.123Z"))))
                    .isEqualTo("2026-07-31 10:34:56.123000000");
        }

    }

    @Nested
    class Reading {

        @ParameterizedTest
        @ValueSource(strings = {
                "2026-07-31T10:34:56.5Z",
                "2026-07-31T10:34:56.500000000Z",
                "2026-07-31 10:34:56.5",
                "2026-07-31T12:34:56.5+02:00"
        })
        void shouldReadInstantFromIsoText(String source) {
            assertThat(StringToInstantConverter.INSTANCE.convert(source))
                    .isEqualTo(Instant.parse("2026-07-31T10:34:56.5Z"));
        }

        @Test
        void shouldReadInstantFromSqliteCurrentTimestamp() {
            assertThat(StringToInstantConverter.INSTANCE.convert("2026-07-31 10:34:56"))
                    .isEqualTo(Instant.parse("2026-07-31T10:34:56Z"));
        }

        @Test
        void shouldReadOffsetDateTimeKeepingStoredOffset() {
            assertThat(StringToOffsetDateTimeConverter.INSTANCE.convert("2026-07-31T12:34:56+02:00"))
                    .isEqualTo(OffsetDateTime.of(2026, 7, 31, 12, 34, 56, 0, ZoneOffset.ofHours(2)));
        }

        @Test
        void shouldReadOffsetDateTimeWithoutOffsetAsUtc() {
            assertThat(StringToOffsetDateTimeConverter.INSTANCE.convert("2026-07-31 10:34:56"))
                    .isEqualTo(OffsetDateTime.of(2026, 7, 31, 10, 34, 56, 0, ZoneOffset.UTC));
        }

        @ParameterizedTest
        @ValueSource(strings = { "2026-07-31T12:34", "2026-07-31T12:34:00.000000000", "2026-07-31 12:34:00" })
        void shouldReadLocalDateTimeFromIsoText(String source) {
            assertThat(StringToLocalDateTimeConverter.INSTANCE.convert(source))
                    .isEqualTo(LocalDateTime.of(2026, 7, 31, 12, 34));
        }

        @Test
        void shouldReadLocalDate() {
            assertThat(StringToLocalDateConverter.INSTANCE.convert("2026-07-31")).isEqualTo(LocalDate.of(2026, 7, 31));
        }

        @ParameterizedTest
        @ValueSource(strings = { "12:34", "12:34:00", "12:34:00.000000000" })
        void shouldReadLocalTime(String source) {
            assertThat(StringToLocalTimeConverter.INSTANCE.convert(source)).isEqualTo(LocalTime.of(12, 34));
        }

        @Test
        void shouldReadZonedDateTimeWithUtcZone() {
            assertThat(StringToZonedDateTimeConverter.INSTANCE.convert("2026-07-31 10:34:56"))
                    .isEqualTo(ZonedDateTime.of(2026, 7, 31, 10, 34, 56, 0, ZoneOffset.UTC));
        }

        @ParameterizedTest
        @ValueSource(strings = { "10:34:00", "10:34:00.000000000", "12:34+02:00" })
        void shouldReadOffsetTime(String source) {
            assertThat(StringToOffsetTimeConverter.INSTANCE.convert(source).withOffsetSameInstant(ZoneOffset.UTC))
                    .isEqualTo(OffsetTime.of(10, 34, 0, 0, ZoneOffset.UTC));
        }

        @Test
        void shouldReadYearFromNumber() {
            assertThat(NumberToYearConverter.INSTANCE.convert(2026)).isEqualTo(Year.of(2026));
        }

        @Test
        void shouldReadYearFromText() {
            assertThat(StringToYearConverter.INSTANCE.convert("2026")).isEqualTo(Year.of(2026));
        }

        @Test
        void shouldReadYearMonth() {
            assertThat(StringToYearMonthConverter.INSTANCE.convert("2026-07")).isEqualTo(YearMonth.of(2026, 7));
        }

        @Test
        void shouldReadDate() {
            assertThat(StringToDateConverter.INSTANCE.convert("2026-07-31 10:34:56.123"))
                    .isEqualTo(Date.from(Instant.parse("2026-07-31T10:34:56.123Z")));
        }

        @Test
        void shouldRejectMalformedText() {
            assertThatThrownBy(() -> StringToInstantConverter.INSTANCE.convert("yesterday"))
                    .isInstanceOf(DateTimeParseException.class);
        }

    }

    @Test
    void shouldRoundTripEveryType() {
        Instant instant = Instant.parse("2026-07-31T10:34:56.123456789Z");
        LocalDateTime localDateTime = LocalDateTime.of(2026, 7, 31, 12, 34, 56, 1);
        LocalTime localTime = LocalTime.of(12, 34, 56, 1);

        assertThat(StringToInstantConverter.INSTANCE.convert(InstantToStringConverter.INSTANCE.convert(instant)))
                .isEqualTo(instant);
        assertThat(StringToLocalDateTimeConverter.INSTANCE.convert(LocalDateTimeToStringConverter.INSTANCE.convert(localDateTime)))
                .isEqualTo(localDateTime);
        assertThat(StringToLocalTimeConverter.INSTANCE.convert(LocalTimeToStringConverter.INSTANCE.convert(localTime)))
                .isEqualTo(localTime);
    }

}
