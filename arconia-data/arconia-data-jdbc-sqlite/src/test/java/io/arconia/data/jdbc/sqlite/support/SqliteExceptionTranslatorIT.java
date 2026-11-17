package io.arconia.data.jdbc.sqlite.support;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for {@link SqliteExceptionTranslator} against a real SQLite database, with
 * the translator auto-configured as it is in an application.
 */
@DataJdbcTest(properties = {
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:sqlite/schema.sql"
})
class SqliteExceptionTranslatorIT {

    @TempDir
    static Path databaseDirectory;

    @DynamicPropertySource
    static void sqliteProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:sqlite:" + databaseDirectory.resolve("test.db") + "?busy_timeout=100");
    }

    @Autowired
    private JdbcAggregateTemplate template;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void shouldTranslateDuplicateKey() {
        template.insert(new Product(1L, "1"));

        assertThatThrownBy(() -> template.insert(new Product(1L, "2")))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void shouldTranslateConstraintViolation() {
        assertThatThrownBy(() -> jdbcClient.sql("INSERT INTO author (book_id, name) VALUES (1, NULL)").update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldTranslateBusyDatabase() {
        // The test transaction holds the write lock, so a write from another connection times out.
        jdbcClient.sql("INSERT INTO product (price) VALUES ('1')").update();

        CompletableFuture<Integer> otherWrite = CompletableFuture
                .supplyAsync(() -> jdbcClient.sql("INSERT INTO product (price) VALUES ('2')").update());

        assertThat(otherWrite).failsWithin(Duration.ofSeconds(10))
                .withThrowableOfType(ExecutionException.class)
                .havingCause()
                .isInstanceOf(CannotAcquireLockException.class);
    }

    record Product(@Id @Nullable Long id, String price) {}

    /**
     * The application the slice is taken from, as a test would have one in the project.
     */
    @SpringBootApplication(proxyBeanMethods = false)
    static class TestApplication {}

}
