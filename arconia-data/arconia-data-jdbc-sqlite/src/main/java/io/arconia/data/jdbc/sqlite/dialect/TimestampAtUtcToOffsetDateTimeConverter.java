package io.arconia.data.jdbc.sqlite.dialect;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

/**
 * Reads a {@link Timestamp} as an {@link OffsetDateTime}. The timestamp is interpreted as UTC;
 * the resulting value represents the same instant with a zero offset.
 *
 * <p>Based on {@code org.springframework.data.relational.core.dialect.TimestampAtUtcToOffsetDateTimeConverter}
 * (Spring Data Relational, Apache-2.0).
 */
@ReadingConverter
enum TimestampAtUtcToOffsetDateTimeConverter implements Converter<Timestamp, OffsetDateTime> {

    INSTANCE;

    @Override
    public OffsetDateTime convert(Timestamp timestamp) {
        return OffsetDateTime.ofInstant(timestamp.toInstant(), ZoneId.of("UTC"));
    }
}
