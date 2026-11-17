# 11. SQLite support for Spring Data JDBC

Date: 2026-05-31

## Status

Accepted

## Context

SQLite runs inside the application process and keeps the whole database in a single file. It suits applications that run as a single instance, such as small services, tools, and applications at the edge, and it needs no database server during development.

Spring Data JDBC supports a fixed set of databases, each with a dialect maintained by the Spring Data team. SQLite support was requested together with a dialect implementation ([spring-data-relational#1769](https://github.com/spring-projects/spring-data-relational/issues/1769), [spring-data-relational#1770](https://github.com/spring-projects/spring-data-relational/pull/1770)), and declined: the team doesn't have the capacity to maintain more databases and suggested hosting the dialect in a separate repository. It plans infrastructure for testing dialects maintained elsewhere ([spring-data-relational#2048](https://github.com/spring-projects/spring-data-relational/issues/2048)).

A library can contribute a dialect to Spring Data JDBC, which picks it based on the database it connects to. Some behavior is decided inside Spring Data JDBC, though, without a way for a dialect to change it. Upserts are rendered only for the databases Spring Data supports, and the team keeps that closed while the design of dialects evolves ([spring-data-relational#2304](https://github.com/spring-projects/spring-data-relational/issues/2304)). Patterns in derived `LIKE` queries are escaped without the `ESCAPE` clause that SQLite needs to honor the escape character. Besides, the SQLite JDBC driver reports errors without an SQL state, which Spring relies on to translate errors into its data access exceptions.

## Decision

Arconia provides SQLite support for Spring Data JDBC in its own module, which contributes a dialect for SQLite and the SQLite JDBC driver. Spring Data JDBC picks the dialect when the application connects to SQLite, so no code is needed, and a starter brings the module in together with the Spring Boot starter for Spring Data JDBC. The dialect describes SQLite as it is: it renders the paging syntax of SQLite, relies on identity columns rather than sequences, and emits no lock clause for pessimistic locks, since SQLite locks the whole database file. It maps the values SQLite has no storage class for, such as booleans and dates, to the ones it has.

Where Spring Data JDBC decides a behavior that SQLite needs to differ, Arconia declares the feature unsupported and documents it, rather than working around the extension points of Spring Data. Upserts fail with an error stating that they aren't supported. Reporting the dialect as PostgreSQL, whose upsert syntax SQLite shares, would give SQLite upserts, but it would misreport the database in a public API and depend on an internal choice that Spring Data has said it may change. SQLite gets those features when Spring Data opens the extension points or changes the behavior.

Arconia also translates the errors of the SQLite JDBC driver based on the SQLite result code, into the same data access exceptions that applications get from the databases Spring supports, such as `DuplicateKeyException` and `CannotAcquireLockException`. The translation applies to Spring Data JDBC, `JdbcClient`, `JdbcTemplate`, and transactions, and an application can replace it with its own.

## Consequences

Applications use SQLite with Spring Data JDBC like the databases Spring Data supports, from a single dependency and without configuration beyond the JDBC URL.

Arconia maintains the dialect and follows the changes Spring Data makes to dialects. The dialect API stays incubating while Spring Data reworks how dialects contribute behavior. Its behavior is tested by Arconia alone until Spring Data provides a way to test dialects maintained elsewhere.

Upserts and escaped `LIKE` patterns in derived queries don't work with SQLite until Spring Data changes, and applications write those queries themselves in the meantime.
