package io.arconia.data.jdbc.sqlite.dialect;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.InstantToStringConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.LocalDateTimeToStringConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.LocalDateToStringConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.LocalTimeToStringConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.OffsetDateTimeToStringConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.StringToInstantConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.StringToLocalDateConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.StringToLocalDateTimeConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.StringToLocalTimeConverter;
import io.arconia.data.jdbc.sqlite.dialect.TemporalConverters.StringToOffsetDateTimeConverter;

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
