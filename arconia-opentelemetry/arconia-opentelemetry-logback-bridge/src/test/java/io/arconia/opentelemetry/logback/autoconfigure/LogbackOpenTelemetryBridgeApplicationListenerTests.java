package io.arconia.opentelemetry.logback.autoconfigure;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.bootstrap.DefaultBootstrapContext;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.ResolvableType;
import org.springframework.mock.env.MockEnvironment;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
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

    @Nested
    class AttributeSelectors {

        private static final String PREFIX = LogbackOpenTelemetryBridgeProperties.CONFIG_PREFIX;

        private final InMemoryLogRecordExporter exporter = InMemoryLogRecordExporter.create();

        private final SdkLoggerProvider loggerProvider = SdkLoggerProvider.builder()
                .addLogRecordProcessor(SimpleLogRecordProcessor.create(exporter))
                .build();

        private final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        @AfterEach
        void tearDown() {
            MDC.clear();
            loggerContext.putProperty("app.name", null);
            loggerContext.putProperty("app.secret", null);
            loggerProvider.close();
        }

        @Test
        void structuredAttributesNotCapturedByDefault() {
            registerAppender(new MockEnvironment());

            LoggerFactory.getLogger("demo").atInfo().addKeyValue("user.name", "alice").log("hello");

            assertThat(exportedAttributes()).doesNotContainKey(AttributeKey.stringKey("user.name"));
        }

        @Test
        void structuredAttributesSelectedByGlob() {
            MockEnvironment env = new MockEnvironment();
            env.setProperty(PREFIX + ".structured-attributes.included", "user.*");
            env.setProperty(PREFIX + ".structured-attributes.excluded", "user.secret");
            registerAppender(env);

            LoggerFactory.getLogger("demo").atInfo()
                    .addKeyValue("user.name", "alice")
                    .addKeyValue("user.secret", "hunter2")
                    .addKeyValue("order.id", "42")
                    .log("hello");

            assertThat(exportedAttributes())
                    .containsEntry(AttributeKey.stringKey("user.name"), "alice")
                    .doesNotContainKey(AttributeKey.stringKey("user.secret"))
                    .doesNotContainKey(AttributeKey.stringKey("order.id"));
        }

        @Test
        void structuredAttributesExcludeOnlyCapturesTheRest() {
            MockEnvironment env = new MockEnvironment();
            env.setProperty(PREFIX + ".structured-attributes.excluded", "*.secret");
            registerAppender(env);

            LoggerFactory.getLogger("demo").atInfo()
                    .addKeyValue("user.name", "alice")
                    .addKeyValue("user.secret", "hunter2")
                    .log("hello");

            assertThat(exportedAttributes())
                    .containsEntry(AttributeKey.stringKey("user.name"), "alice")
                    .doesNotContainKey(AttributeKey.stringKey("user.secret"));
        }

        @Test
        void mdcAttributesNotCapturedByDefault() {
            registerAppender(new MockEnvironment());

            MDC.put("tenant.id", "acme");
            LoggerFactory.getLogger("demo").info("hello");

            assertThat(exportedAttributes()).doesNotContainKey(AttributeKey.stringKey("tenant.id"));
        }

        @Test
        void mdcAttributesSelectedByGlob() {
            MockEnvironment env = new MockEnvironment();
            env.setProperty(PREFIX + ".mdc-attributes.included", "tenant.*,request.id");
            registerAppender(env);

            MDC.put("tenant.id", "acme");
            MDC.put("request.id", "r-1");
            MDC.put("session.id", "s-1");
            LoggerFactory.getLogger("demo").info("hello");

            assertThat(exportedAttributes())
                    .containsEntry(AttributeKey.stringKey("tenant.id"), "acme")
                    .containsEntry(AttributeKey.stringKey("request.id"), "r-1")
                    .doesNotContainKey(AttributeKey.stringKey("session.id"));
        }

        @Test
        void loggerContextAttributesNotCapturedByDefault() {
            registerAppender(new MockEnvironment());

            loggerContext.putProperty("app.name", "demo");
            LoggerFactory.getLogger("demo").info("hello");

            assertThat(exportedAttributes()).doesNotContainKey(AttributeKey.stringKey("app.name"));
        }

        @Test
        void loggerContextAttributesSelectedByGlob() {
            MockEnvironment env = new MockEnvironment();
            env.setProperty(PREFIX + ".logger-context-attributes.included", "*");
            env.setProperty(PREFIX + ".logger-context-attributes.excluded", "*.secret");
            registerAppender(env);

            loggerContext.putProperty("app.name", "demo");
            loggerContext.putProperty("app.secret", "hunter2");
            LoggerFactory.getLogger("demo").info("hello");

            assertThat(exportedAttributes())
                    .containsEntry(AttributeKey.stringKey("app.name"), "demo")
                    .doesNotContainKey(AttributeKey.stringKey("app.secret"));
        }

        private void registerAppender(MockEnvironment environment) {
            listener.onApplicationEvent(createEvent(environment));
            OpenTelemetry openTelemetry = OpenTelemetrySdk.builder().setLoggerProvider(loggerProvider).build();
            OpenTelemetryAppender.install(openTelemetry);
        }

        private Map<AttributeKey<?>, Object> exportedAttributes() {
            List<LogRecordData> logRecords = exporter.getFinishedLogRecordItems();
            assertThat(logRecords).hasSize(1);
            return logRecords.get(0).getAttributes().asMap();
        }

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
