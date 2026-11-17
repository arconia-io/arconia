# 12. SQLite date and time storage

Date: 2026-11-01

## Status

Accepted

## Context

SQLite has no storage class for dates and times. Its date and time functions work with values stored as ISO-8601 text, as Julian day numbers, or as Unix time, and leave the choice to the application ([Date and Time Datatype](https://www.sqlite.org/datatype3.html#date_and_time_datatype)). SQLite treats every date and time as UTC, and compares text values character by character.

The SQLite dialect for Spring Data JDBC (see [11. SQLite support for Spring Data JDBC](0011-sqlite-support-for-spring-data-jdbc.md)) initially relied on the defaults of Spring Data JDBC and of the SQLite JDBC driver. Values were stored in different formats depending on their type, some as Unix time in milliseconds, and most types couldn't be read back.

The format is a contract with the data that applications store. Changing it once applications depend on it means migrating their databases.

## Decision

Arconia stores date and time values as text, in the format of the SQLite date and time functions, such as `datetime()` and `CURRENT_TIMESTAMP`: the date, a space, the time, and no offset. Values written by the application and values produced by SQLite, such as the defaults of a column, are then in the same format, so they sort and compare correctly together in queries. The fraction of the seconds always has nine digits, so values keep the full precision of the Java types and values written by the application sort correctly among themselves. Text was chosen over numbers because it is what the SQLite functions produce, and it is readable in any SQLite tool.

Values that represent an instant, including the ones with an offset or a time zone, are converted to UTC, since SQLite treats every date and time as UTC. The offset and the time zone aren't stored, and the values are read back with a UTC offset. PostgreSQL inspired this choice: its `timestamp with time zone` type stores the instant and not the offset. Local dates and times are stored as they are, a year as an integer, and a year with a month as text such as `2026-07`. The legacy `java.util.Date` and `java.sql` types are stored in the same formats as their `java.time` counterparts.

Reading accepts what SQLite and other tools commonly write: a `T` or a space between the date and the time, any number of digits in the fraction, and an optional offset, where a missing offset means UTC. Values stored as numbers aren't read as dates and times, so that a column holds one format only.

## Consequences

Dates and times written by the application work with the SQLite date and time functions, sort correctly, and are readable without conversion.

The offset and the time zone of a value are lost. Applications that need them store them in a separate column.

Values without a fraction, as the SQLite functions produce them, compare as earlier than the same instant written by the application with a fraction, so a query comparing for equality with such a value needs to normalize one side.

The SQLite JDBC driver doesn't apply the format, so values written with `JdbcClient` or `JdbcTemplate` must be formatted by the application.
