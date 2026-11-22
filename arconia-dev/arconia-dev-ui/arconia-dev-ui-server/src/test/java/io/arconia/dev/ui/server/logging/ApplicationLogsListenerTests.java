package io.arconia.dev.ui.server.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.bootstrap.DefaultBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.mock.env.MockEnvironment;

import io.arconia.core.bootstrap.BootstrapMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ApplicationLogsListener}.
 */
class ApplicationLogsListenerTests {

    private final ApplicationLogsListener listener = new ApplicationLogsListener();

    private final Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

    @BeforeEach
    @AfterEach
    void stopCapturing() {
        this.rootLogger.detachAppender(LogbackCapture.APPENDER_NAME);
        System.clearProperty(BootstrapMode.SYSTEM_PROPERTY);
    }

    @Test
    void logsAreCapturedInDevMode() {
        System.setProperty(BootstrapMode.SYSTEM_PROPERTY, "dev");

        this.listener.onApplicationEvent(event(new MockEnvironment()));

        assertThat(this.rootLogger.getAppender(LogbackCapture.APPENDER_NAME)).isNotNull();
        assertThat(ApplicationLogs.get().isCapturing()).isTrue();
    }

    /**
     * Spring Boot DevTools restarts the application in the same JVM, and Spring Boot sets up
     * Logback again, which removes every appender. The capture is put back with the logs of the
     * run that ended, followed by a marker.
     */
    @Test
    void logsAreCapturedAcrossRestarts() throws Exception {
        System.setProperty(BootstrapMode.SYSTEM_PROPERTY, "dev");
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = loggerContext.getLogger(ApplicationLogsListenerTests.class);
        try {
            this.listener.onApplicationEvent(event(new MockEnvironment()));
            logger.info("Before the restart");

            loggerContext.reset();
            logger.info("While Logback is set up again");
            this.listener.onApplicationEvent(event(new MockEnvironment()));
            logger.info("After the restart");

            assertThat(this.rootLogger.getAppender(LogbackCapture.APPENDER_NAME)).isNotNull();
            assertThat(ApplicationLogs.get().lines()).extracting(LogLine::message)
                    .containsSubsequence("Before the restart", "The application was restarted", "After the restart")
                    .doesNotContain("While Logback is set up again");
        } finally {
            // The other tests log as Logback is set up by default.
            loggerContext.reset();
            new ContextInitializer(loggerContext).autoConfig();
        }
    }

    @Test
    void logsAreNotCapturedOutsideDevMode() {
        System.setProperty(BootstrapMode.SYSTEM_PROPERTY, "prod");

        this.listener.onApplicationEvent(event(new MockEnvironment()));

        assertThat(this.rootLogger.getAppender(LogbackCapture.APPENDER_NAME)).isNull();
    }

    @Test
    void logsAreNotCapturedWhenTheDevUiIsTurnedOff() {
        System.setProperty(BootstrapMode.SYSTEM_PROPERTY, "dev");

        this.listener.onApplicationEvent(event(new MockEnvironment().withProperty("arconia.dev.ui.enabled", "false")));

        assertThat(this.rootLogger.getAppender(LogbackCapture.APPENDER_NAME)).isNull();
    }

    @Test
    void logsAreNotCapturedInAnApplicationTheDevUiIsNotServedIn() {
        System.setProperty(BootstrapMode.SYSTEM_PROPERTY, "dev");
        SpringApplication application = new SpringApplication();
        application.setWebApplicationType(WebApplicationType.NONE);

        this.listener.onApplicationEvent(
                new ApplicationEnvironmentPreparedEvent(new DefaultBootstrapContext(), application, new String[0], new MockEnvironment()));

        assertThat(this.rootLogger.getAppender(LogbackCapture.APPENDER_NAME)).isNull();
    }

    @Test
    void listenerRunsAfterTheApplicationHasSetUpItsLogging() {
        assertThat(this.listener.getOrder()).isGreaterThan(LoggingApplicationListener.DEFAULT_ORDER);
    }

    private static ApplicationEnvironmentPreparedEvent event(MockEnvironment environment) {
        return new ApplicationEnvironmentPreparedEvent(new DefaultBootstrapContext(), new SpringApplication(), new String[0], environment);
    }

}
