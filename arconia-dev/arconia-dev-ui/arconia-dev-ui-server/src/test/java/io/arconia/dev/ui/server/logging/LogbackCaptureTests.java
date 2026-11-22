package io.arconia.dev.ui.server.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LogbackCapture}.
 */
class LogbackCaptureTests {

    private final ApplicationLogs logs = new ApplicationLogs();

    private final Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

    private final Logger logger = (Logger) LoggerFactory.getLogger("com.example.shop.CatalogService");

    @AfterEach
    void stopCapturing() {
        this.rootLogger.detachAppender(LogbackCapture.APPENDER_NAME);
        this.logger.setLevel(null);
    }

    @Test
    void lineLoggedByTheApplicationIsCaptured() {
        this.rootLogger.detachAppender(LogbackCapture.APPENDER_NAME);
        assertThat(LogbackCapture.start(this.logs)).isTrue();
        this.logger.setLevel(Level.INFO);

        this.logger.info("Loaded shelf {} with {} books", 4, 12);

        LogLine line = this.logs.lines().getLast();
        assertThat(line.level()).isEqualTo("INFO");
        assertThat(line.logger()).isEqualTo("com.example.shop.CatalogService");
        assertThat(line.message()).isEqualTo("Loaded shelf 4 with 12 books");
        assertThat(line.time()).isPositive();
        assertThat(line.stackTrace()).isNull();
        assertThat(line.marker()).isFalse();
    }

    @Test
    void exceptionLoggedWithLineIsCapturedAsItsStackTrace() {
        this.rootLogger.detachAppender(LogbackCapture.APPENDER_NAME);
        LogbackCapture.start(this.logs);

        this.logger.error("Cannot load the shelf", new IllegalStateException("The catalog is closed", new IllegalArgumentException("No such shelf")));

        LogLine line = this.logs.lines().getLast();
        assertThat(line.level()).isEqualTo("ERROR");
        assertThat(line.message()).isEqualTo("Cannot load the shelf");
        assertThat(line.stackTrace())
                .startsWith("java.lang.IllegalStateException: The catalog is closed")
                .contains("\tat " + LogbackCaptureTests.class.getName())
                .contains("Caused by: java.lang.IllegalArgumentException: No such shelf");
    }

    @Test
    void textThatIsTooLongToKeepIsCut() {
        this.rootLogger.detachAppender(LogbackCapture.APPENDER_NAME);
        LogbackCapture.start(this.logs);
        this.logger.setLevel(Level.INFO);

        this.logger.info("x".repeat(LogbackCapture.MAX_LENGTH + 500));

        assertThat(this.logs.lines().getLast().message())
                .hasSize(LogbackCapture.MAX_LENGTH + "... (500 more characters)".length())
                .endsWith("x... (500 more characters)");
        assertThat(LogbackCapture.shortened("short")).isEqualTo("short");
    }

    @Test
    void captureIsStartedOnce() {
        this.rootLogger.detachAppender(LogbackCapture.APPENDER_NAME);

        assertThat(LogbackCapture.start(this.logs)).isTrue();
        assertThat(LogbackCapture.start(this.logs)).isFalse();

        this.logger.warn("Shelf is almost empty");
        assertThat(this.logs.lines()).filteredOn(line -> line.message().equals("Shelf is almost empty")).hasSize(1);
    }

}
