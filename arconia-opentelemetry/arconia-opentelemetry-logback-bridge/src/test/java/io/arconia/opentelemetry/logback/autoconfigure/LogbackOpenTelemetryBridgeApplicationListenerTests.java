package io.arconia.opentelemetry.logback.autoconfigure;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.bootstrap.DefaultBootstrapContext;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.ResolvableType;
import org.springframework.mock.env.MockEnvironment;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;

import io.arconia.opentelemetry.autoconfigure.OpenTelemetryProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link LogbackOpenTelemetryBridgeApplicationListener}.
 */
class LogbackOpenTelemetryBridgeApplicationListenerTests {

    private final LogbackOpenTelemetryBridgeApplicationListener listener = new LogbackOpenTelemetryBridgeApplicationListener();

    private final Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    @AfterEach
    void tearDown() {
        List<Appender<ILoggingEvent>> toRemove = new ArrayList<>();
        Iterator<Appender<ILoggingEvent>> it = rootLogger.iteratorForAppenders();
        while (it.hasNext()) {
            Appender<ILoggingEvent> appender = it.next();
            if (appender instanceof OpenTelemetryAppender) {
                toRemove.add(appender);
            }
        }
        toRemove.forEach(rootLogger::detachAppender);
    }

    @Test
    void supportsApplicationEnvironmentPreparedEventType() {
        ResolvableType eventType = ResolvableType.forClass(ApplicationEnvironmentPreparedEvent.class);
        assertThat(listener.supportsEventType(eventType)).isTrue();
    }

    @Test
    void doesNotSupportOtherEventTypes() {
        ResolvableType eventType = ResolvableType.forClass(ContextRefreshedEvent.class);
        assertThat(listener.supportsEventType(eventType)).isFalse();
    }

    @Test
    void supportsApplicationContextSourceType() {
        assertThat(listener.supportsSourceType(ApplicationContext.class)).isTrue();
    }

    @Test
    void supportsSpringApplicationSourceType() {
        assertThat(listener.supportsSourceType(SpringApplication.class)).isTrue();
    }

    @Test
    void doesNotSupportNullSourceType() {
        assertThat(listener.supportsSourceType(null)).isFalse();
    }

    @Test
    void doesNotSupportUnrelatedSourceType() {
        assertThat(listener.supportsSourceType(String.class)).isFalse();
    }

    @Test
    void orderIsAfterLoggingApplicationListener() {
        assertThat(listener.getOrder()).isGreaterThan(LoggingApplicationListener.DEFAULT_ORDER);
    }

    @Test
    void appenderRegisteredWithDefaultConfiguration() {
        listener.onApplicationEvent(createEvent(new MockEnvironment()));
        assertThat(hasOpenTelemetryAppender()).isTrue();
    }

    @Test
    void registeredAppenderIsStarted() {
        listener.onApplicationEvent(createEvent(new MockEnvironment()));
        assertThat(findOpenTelemetryAppender()).isNotNull().satisfies(appender ->
                assertThat(appender.isStarted()).isTrue()
        );
    }

    @Test
    void appenderNotRegisteredWhenOpenTelemetryDisabled() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty(OpenTelemetryProperties.CONFIG_PREFIX + ".enabled", "false");
        listener.onApplicationEvent(createEvent(env));
        assertThat(hasOpenTelemetryAppender()).isFalse();
    }

    @Test
    void appenderNotRegisteredWhenBridgeDisabled() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty(LogbackOpenTelemetryBridgeProperties.CONFIG_PREFIX + ".enabled", "false");
        listener.onApplicationEvent(createEvent(env));
        assertThat(hasOpenTelemetryAppender()).isFalse();
    }

    @Test
    void appenderNotRegisteredForNonEnvironmentPreparedEvent() {
        listener.onApplicationEvent(new ContextRefreshedEvent(mock(ApplicationContext.class)));
        assertThat(hasOpenTelemetryAppender()).isFalse();
    }

    private ApplicationEnvironmentPreparedEvent createEvent(MockEnvironment environment) {
        return new ApplicationEnvironmentPreparedEvent(
                new DefaultBootstrapContext(),
                mock(SpringApplication.class),
                new String[0],
                environment);
    }

    private boolean hasOpenTelemetryAppender() {
        return findOpenTelemetryAppender() != null;
    }

    private OpenTelemetryAppender findOpenTelemetryAppender() {
        Iterator<Appender<ILoggingEvent>> it = rootLogger.iteratorForAppenders();
        while (it.hasNext()) {
            Appender<ILoggingEvent> appender = it.next();
            if (appender instanceof OpenTelemetryAppender otelAppender) {
                return otelAppender;
            }
        }
        return null;
    }

}
