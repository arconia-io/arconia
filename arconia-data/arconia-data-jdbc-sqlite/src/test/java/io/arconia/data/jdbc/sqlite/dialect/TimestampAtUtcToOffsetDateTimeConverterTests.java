package io.arconia.data.jdbc.sqlite.dialect;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TimestampAtUtcToOffsetDateTimeConverter}.
 */
class TimestampAtUtcToOffsetDateTimeConverterTests {

    @Test
    void convertsTimestampToOffsetDateTimeAtUtc() {
        Instant instant = Instant.parse("2026-05-31T12:34:56Z");
        Timestamp timestamp = Timestamp.from(instant);

        OffsetDateTime result = TimestampAtUtcToOffsetDateTimeConverter.INSTANCE.convert(timestamp);

        assertThat(result).isNotNull();
        assertThat(result.getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(result.toInstant()).isEqualTo(instant);
    }

    @Test
    void preservesNanosecondPrecision() {
        Timestamp timestamp = Timestamp.from(Instant.parse("2026-05-31T12:34:56.123456789Z"));

        OffsetDateTime result = TimestampAtUtcToOffsetDateTimeConverter.INSTANCE.convert(timestamp);

        assertThat(result.getNano()).isEqualTo(123_456_789);
    }

    @Test
    void convertsEpoch() {
        Timestamp timestamp = Timestamp.from(Instant.EPOCH);

        OffsetDateTime result = TimestampAtUtcToOffsetDateTimeConverter.INSTANCE.convert(timestamp);

        assertThat(result).isEqualTo(OffsetDateTime.of(LocalDateTime.of(1970, 1, 1, 0, 0, 0), ZoneOffset.UTC));
    }

    @Test
    void nullThrowsException() {
        assertThatThrownBy(() -> TimestampAtUtcToOffsetDateTimeConverter.INSTANCE.convert(null))
                .isInstanceOf(NullPointerException.class);
    }

}
