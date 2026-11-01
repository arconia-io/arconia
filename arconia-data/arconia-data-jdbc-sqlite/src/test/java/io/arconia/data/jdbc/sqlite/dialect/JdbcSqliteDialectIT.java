package io.arconia.data.jdbc.sqlite.dialect;

import java.nio.file.Path;
import java.time.LocalDateTime;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
            List<Book> books = template.findAll(Book.class, PageRequest.of(0, 2, Sort.by("title"))).getContent();

            assertThat(books).extracting(Book::title).containsExactly("A", "B");
        }

        @Test
        void shouldApplyLimitAndOffset() {
            List<Book> books = template.findAll(Book.class, PageRequest.of(1, 2, Sort.by("title"))).getContent();

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
            LocalDateTime publishedAt = LocalDateTime.of(2026, 5, 31, 12, 34, 56, 123_456_789);
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

    interface BookRepository extends ListCrudRepository<Book, Long> {

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
