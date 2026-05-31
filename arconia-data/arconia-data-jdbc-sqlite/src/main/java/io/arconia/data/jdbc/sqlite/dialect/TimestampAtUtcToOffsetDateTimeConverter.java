package io.arconia.data.jdbc.sqlite.dialect;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

/**
 * A reading convert to convert {@link Timestamp} to {@link OffsetDateTime}. For the conversion the {@link Timestamp}
 * gets considered to be at UTC and the result of the conversion will have an offset of 0 and represent the same
 * instant.
 * <p>
 * Based on org.springframework.data.relational.core.dialect.TimestampAtUtcToOffsetDateTimeConverter
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
