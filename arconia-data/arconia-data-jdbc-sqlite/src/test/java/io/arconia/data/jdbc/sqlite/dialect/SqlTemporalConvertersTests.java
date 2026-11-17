package io.arconia.data.jdbc.sqlite.dialect;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.arconia.data.jdbc.sqlite.dialect.SqlTemporalConverters.DateToStringConverter;
import io.arconia.data.jdbc.sqlite.dialect.SqlTemporalConverters.StringToDateConverter;
import io.arconia.data.jdbc.sqlite.dialect.SqlTemporalConverters.StringToTimeConverter;
import io.arconia.data.jdbc.sqlite.dialect.SqlTemporalConverters.StringToTimestampConverter;
import io.arconia.data.jdbc.sqlite.dialect.SqlTemporalConverters.TimeToStringConverter;
import io.arconia.data.jdbc.sqlite.dialect.SqlTemporalConverters.TimestampToStringConverter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SqlTemporalConverters}.
 */
class SqlTemporalConvertersTests {

    private static final Instant INSTANT = Instant.parse("2026-07-31T10:34:56.123456789Z");

    @Nested
    class Writing {

        @Test
        void shouldWriteTimestampAsUtcText() {
            assertThat(TimestampToStringConverter.INSTANCE.convert(Timestamp.from(INSTANT)))
                    .isEqualTo("2026-07-31 10:34:56.123456789");
        }

        @Test
        void shouldWriteDateAsLocalDateText() {
            assertThat(DateToStringConverter.INSTANCE.convert(Date.valueOf(LocalDate.of(2026, 7, 31))))
                    .isEqualTo("2026-07-31");
        }

        @Test
        void shouldWriteTimeAsLocalTimeText() {
            assertThat(TimeToStringConverter.INSTANCE.convert(Time.valueOf(LocalTime.of(12, 34, 56))))
                    .isEqualTo("12:34:56.000000000");
        }

    }

    @Nested
    class Reading {

        @Test
        void shouldReadTimestampKeepingNanos() {
            assertThat(StringToTimestampConverter.INSTANCE.convert("2026-07-31 10:34:56.123456789"))
                    .isEqualTo(Timestamp.from(INSTANT));
        }

        @Test
        void shouldReadDate() {
            assertThat(StringToDateConverter.INSTANCE.convert("2026-07-31"))
                    .isEqualTo(Date.valueOf(LocalDate.of(2026, 7, 31)));
        }

        @Test
        void shouldReadTimeDroppingFraction() {
            assertThat(StringToTimeConverter.INSTANCE.convert("12:34:56.789000000"))
                    .isEqualTo(Time.valueOf(LocalTime.of(12, 34, 56)));
        }

    }

}
