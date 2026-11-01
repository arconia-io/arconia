package io.arconia.data.jdbc.sqlite.dialect;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.jdbc.core.dialect.JdbcDialect;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.sql.LockMode;
import org.springframework.data.relational.repository.Lock;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Integration tests for {@link JdbcSqliteDialect} against a real SQLite database, with the
 * dialect resolved by Spring Data JDBC as it is in an application.
 */
@DataJdbcTest(properties = {
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:sqlite/schema.sql"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JdbcSqliteDialectIT {

    // A file-based database, since each pooled connection to an in-memory one sees a distinct database.
    @TempDir
    static Path databaseDirectory;

    @DynamicPropertySource
    static void sqliteProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + databaseDirectory.resolve("test.db"));
    }

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private JdbcAggregateTemplate template;

    @Autowired
    private JdbcDialect dialect;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void shouldResolveSqliteDialect() {
        assertThat(dialect).isSameAs(JdbcSqliteDialect.INSTANCE);
    }

    @Nested
    class Paging {

        @BeforeEach
        void setUp() {
            bookRepository.saveAll(List.of(book("A"), book("B"), book("C"), book("D"), book("E")));
        }

        @Test
        void shouldApplyLimit() {
            List<Book> books = bookRepository.findAll(PageRequest.of(0, 2, Sort.by("title"))).getContent();

            assertThat(books).extracting(Book::title).containsExactly("A", "B");
        }

        @Test
        void shouldApplyLimitAndOffset() {
            List<Book> books = bookRepository.findAll(PageRequest.of(1, 2, Sort.by("title"))).getContent();

            assertThat(books).extracting(Book::title).containsExactly("C", "D");
        }

        @Test
        void shouldApplyOffsetWithoutLimit() {
            List<Book> books = template.findAll(Query.empty().sort(Sort.by("title")).offset(3), Book.class);

            assertThat(books).extracting(Book::title).containsExactly("D", "E");
        }

    }

    @Nested
    class Sorting {

        @BeforeEach
        void setUp() {
            bookRepository.saveAll(List.of(book("B"), book(null), book("A")));
        }

        @Test
        void shouldSortNullsFirst() {
            List<Book> books = template.findAll(Book.class, Sort.by(Sort.Order.asc("title").nullsFirst()));

            assertThat(books).extracting(Book::title).containsExactly(null, "A", "B");
        }

        @Test
        void shouldSortNullsLast() {
            List<Book> books = template.findAll(Book.class, Sort.by(Sort.Order.asc("title").nullsLast()));

            assertThat(books).extracting(Book::title).containsExactly("A", "B", null);
        }

    }

    @Nested
    class Identity {

        @Test
        void shouldPopulateGeneratedId() {
            Book saved = bookRepository.save(book("A"));

            assertThat(saved.id()).isNotNull();
        }

        @Test
        void shouldPopulateGeneratedIdsInBatch() {
            List<Book> saved = template.insertAll(List.of(book("A"), book("B"), book("C")));

            assertThat(saved).extracting(Book::id).doesNotContainNull().doesNotHaveDuplicates();
        }

        @Test
        void shouldIncrementVersionOnUpdate() {
            Book saved = bookRepository.save(book("A"));

            Book updated = bookRepository.save(saved.withTitle("B"));

            assertThat(updated.version()).isEqualTo(saved.version() + 1);
        }

    }

    @Nested
    class Types {

        @Test
        void shouldRoundTripValues() {
            UUID isbn = UUID.randomUUID();
            LocalDateTime publishedAt = LocalDateTime.of(2026, 7, 31, 12, 34, 56, 123_456_789);
            Book saved = bookRepository.save(new Book(null, "A", true, isbn, publishedAt, null, Set.of()));

            Book found = bookRepository.findById(saved.id()).orElseThrow();

            assertThat(found.available()).isTrue();
            assertThat(found.isbn()).isEqualTo(isbn);
            assertThat(found.publishedAt()).isEqualTo(publishedAt);
        }

        @Test
        void shouldReadFalse() {
            Book saved = bookRepository.save(new Book(null, "A", false, null, null, null, Set.of()));

            assertThat(bookRepository.findById(saved.id())).get().extracting(Book::available).isEqualTo(false);
        }

        @Test
        void shouldKeepDecimalPrecisionInTextColumn() {
            BigDecimal price = new BigDecimal("12345678901234567890.123456789");
            Product saved = template.insert(new Product(null, price));

            assertThat(template.findById(saved.id(), Product.class)).extracting(Product::price).isEqualTo(price);
        }

    }

    @Nested
    class Temporal {

        private static final Instant INSTANT = Instant.parse("2026-07-31T10:34:56.123456789Z");

        @Test
        void shouldRoundTripTemporalValues() {
            OffsetDateTime offsetDateTime = INSTANT.atOffset(ZoneOffset.ofHours(2));
            Event saved = template.insert(new Event(null, INSTANT, offsetDateTime, LocalDateTime.of(2026, 7, 31, 12, 34),
                    LocalDate.of(2026, 7, 31), LocalTime.of(12, 34, 56, 123_456_789)));

            Event found = template.findById(saved.id(), Event.class);

            assertThat(found).isNotNull();
            assertThat(found.instant()).isEqualTo(INSTANT);
            assertThat(found.offsetDateTime()).isEqualTo(INSTANT.atOffset(ZoneOffset.UTC));
            assertThat(found.localDateTime()).isEqualTo(LocalDateTime.of(2026, 7, 31, 12, 34));
            assertThat(found.localDate()).isEqualTo(LocalDate.of(2026, 7, 31));
            assertThat(found.localTime()).isEqualTo(LocalTime.of(12, 34, 56, 123_456_789));
        }

        @Test
        void shouldStoreUtcText() {
            template.insert(new Event(null, INSTANT, INSTANT.atOffset(ZoneOffset.ofHours(2)), null, null, null));

            assertThat(jdbcClient.sql("SELECT instant || '|' || offset_date_time FROM event").query(String.class).single())
                    .isEqualTo("2026-07-31 10:34:56.123456789|2026-07-31 10:34:56.123456789");
        }

        @Test
        void shouldSortTogetherWithSqliteTimestamps() {
            jdbcClient.sql("INSERT INTO event (instant) VALUES ('2026-07-31 23:00:00')").update();
            template.insert(new Event(null, Instant.parse("2026-07-31T01:00:00Z"), null, null, null, null));

            List<Event> events = template.findAll(Event.class, Sort.by("instant"));

            assertThat(events).extracting(Event::instant)
                    .containsExactly(Instant.parse("2026-07-31T01:00:00Z"), Instant.parse("2026-07-31T23:00:00Z"));
        }

        @Test
        void shouldCompareWithSqliteDateFunctions() {
            template.insertAll(List.of(new Event(null, Instant.parse("2026-07-30T11:00:00Z"), null, null, null, null),
                    new Event(null, Instant.parse("2026-07-31T10:00:00Z"), null, null, null, null)));

            List<String> recent = jdbcClient.sql("SELECT instant FROM event WHERE instant > datetime('2026-07-31 12:00:00', '-1 day')")
                    .query(String.class).list();

            assertThat(recent).containsExactly("2026-07-31 10:00:00.000000000");
        }

        @Test
        void shouldSortInstantsChronologically() {
            Instant whole = Instant.parse("2026-07-31T10:34:56Z");
            Instant fractional = Instant.parse("2026-07-31T10:34:56.5Z");
            template.insertAll(List.of(new Event(null, fractional, null, null, null, null),
                    new Event(null, whole, null, null, null, null)));

            List<Event> events = template.findAll(Event.class, Sort.by("instant"));

            assertThat(events).extracting(Event::instant).containsExactly(whole, fractional);
        }

        @Test
        void shouldReadSqliteCurrentTimestamp() {
            jdbcClient.sql("""
                    INSERT INTO event (instant, offset_date_time, local_date_time, local_date, local_time)
                    VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_DATE, CURRENT_TIME)
                    """).update();

            Event found = template.findAll(Event.class).get(0);

            assertThat(found.instant()).isCloseTo(Instant.now(), within(1, ChronoUnit.MINUTES));
            assertThat(found.offsetDateTime()).isEqualTo(found.instant().atOffset(ZoneOffset.UTC));
            assertThat(found.localDateTime()).isEqualTo(LocalDateTime.ofInstant(found.instant(), ZoneOffset.UTC));
            assertThat(found.localDate()).isNotNull();
            assertThat(found.localTime()).isNotNull();
        }

    }

    @Nested
    class Aggregates {

        @Test
        void shouldLoadMappedCollection() {
            Book saved = bookRepository.save(new Book(null, "A", true, null, null, null,
                    Set.of(new Author("Ada"), new Author("Grace"))));

            Book found = bookRepository.findById(saved.id()).orElseThrow();

            assertThat(found.authors()).extracting(Author::name).containsExactlyInAnyOrder("Ada", "Grace");
        }

        @Test
        void shouldCheckExistence() {
            Book saved = bookRepository.save(book("A"));

            assertThat(bookRepository.existsById(saved.id())).isTrue();
        }

    }

    @Nested
    class Locking {

        @Test
        void shouldRunPessimisticLockQuery() {
            bookRepository.save(book("A"));

            assertThat(bookRepository.findByTitle("A")).hasSize(1);
        }

    }

    @Nested
    class Upsert {

        @Test
        void shouldRejectUpsert() {
            assertThatThrownBy(() -> template.upsert(new Book(1L, "A", true, null, null, null, Set.of())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Upsert is not supported by dialect");
        }

    }

    private static Book book(@Nullable String title) {
        return new Book(null, title, true, null, null, null, Set.of());
    }

    record Book(@Id @Nullable Long id, @Nullable String title, boolean available, @Nullable UUID isbn,
            @Nullable LocalDateTime publishedAt, @Version @Nullable Integer version,
            @MappedCollection(idColumn = "book_id") Set<Author> authors) {

        Book withTitle(String title) {
            return new Book(id, title, available, isbn, publishedAt, version, authors);
        }

    }

    record Author(String name) {}

    record Product(@Id @Nullable Long id, BigDecimal price) {}

    record Event(@Id @Nullable Long id, @Nullable Instant instant, @Nullable OffsetDateTime offsetDateTime,
            @Nullable LocalDateTime localDateTime, @Nullable LocalDate localDate, @Nullable LocalTime localTime) {}

    interface BookRepository extends ListCrudRepository<Book, Long>, ListPagingAndSortingRepository<Book, Long> {

        @Lock(LockMode.PESSIMISTIC_WRITE)
        List<Book> findByTitle(String title);

    }

    /**
     * The application the slice is taken from, as a test would have one in the project.
     */
    @SpringBootApplication(proxyBeanMethods = false)
    @EnableJdbcRepositories(considerNestedRepositories = true)
    static class TestApplication {}

}
