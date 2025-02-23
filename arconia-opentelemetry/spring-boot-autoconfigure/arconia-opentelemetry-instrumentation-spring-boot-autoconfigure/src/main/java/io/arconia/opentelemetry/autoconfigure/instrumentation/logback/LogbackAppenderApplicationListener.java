package io.arconia.opentelemetry.autoconfigure.instrumentation.logback;

import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;

import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.ResolvableType;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import ch.qos.logback.classic.Logger;

/**
 * An {@link ApplicationListener} that configures the OpenTelemetry Logback appender
 * with the root Logback logger.
 */
public class LogbackAppenderApplicationListener implements GenericApplicationListener {

    private static final Class<?>[] EVENT_TYPES = {ApplicationEnvironmentPreparedEvent.class};
    private static final Class<?>[] SOURCE_TYPES = {ApplicationContext.class, SpringApplication.class};

    @Override
    public boolean supportsEventType(ResolvableType eventType) {
        Assert.notNull(eventType, "eventType cannot be null");
        return isAssignableFrom(eventType.getRawClass(), EVENT_TYPES);
    }

    @Override
    public boolean supportsSourceType(@Nullable Class<?> sourceType) {
        return isAssignableFrom(sourceType, SOURCE_TYPES);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (!shouldRegisterLogbackAppender()) {
            return;
        }

        if (!(event instanceof ApplicationEnvironmentPreparedEvent applicationEvent)) {
            return;
        }

        Binder binder = Binder.get(applicationEvent.getEnvironment());
        if (!isOpenTelemetryEnabled(binder) || !isLogbackInstrumentationEnabled(binder)) {
            return;
        }

        OpenTelemetryAppender openTelemetryAppender = new OpenTelemetryAppender();
        configureOpenTelemetryAppender(openTelemetryAppender, binder);
        openTelemetryAppender.start();

        Logger rootLogbackLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogbackLogger.addAppender(openTelemetryAppender);
    }

    private void configureOpenTelemetryAppender(OpenTelemetryAppender openTelemetryAppender, Binder binder) {
        boolean captureArguments = binder
                .bind(LogbackAppenderProperties.CONFIG_PREFIX + ".capture-arguments",
                        Boolean.class)
                .orElse(false);
        openTelemetryAppender.setCaptureArguments(captureArguments);

        boolean captureCodeAttributes = binder
                .bind(LogbackAppenderProperties.CONFIG_PREFIX + ".capture-code-attributes",
                        Boolean.class)
                .orElse(false);
        openTelemetryAppender.setCaptureCodeAttributes(captureCodeAttributes);

        boolean captureExperimentalAttributes = binder
                .bind(LogbackAppenderProperties.CONFIG_PREFIX + ".capture-experimental-attributes",
                        Boolean.class)
                .orElse(false);
        openTelemetryAppender.setCaptureExperimentalAttributes(captureExperimentalAttributes);

        boolean captureKeyValuePairAttributes = binder
                .bind(LogbackAppenderProperties.CONFIG_PREFIX + ".capture-key-value-pair-attributes",
                        Boolean.class)
                .orElse(false);
        openTelemetryAppender.setCaptureKeyValuePairAttributes(captureKeyValuePairAttributes);

        boolean captureLoggerContext = binder
                .bind(LogbackAppenderProperties.CONFIG_PREFIX + ".capture-logger-context",
                        Boolean.class)
                .orElse(false);
        openTelemetryAppender.setCaptureLoggerContext(captureLoggerContext);

        boolean captureLogstashAttributes = binder
                .bind(LogbackAppenderProperties.CONFIG_PREFIX + ".capture-logstash-attributes",
                        Boolean.class)
                .orElse(false);
        openTelemetryAppender.setCaptureLogstashAttributes(captureLogstashAttributes);

        boolean captureMarkerAttribute = binder
                .bind(LogbackAppenderProperties.CONFIG_PREFIX + ".capture-marker-attribute",
                        Boolean.class)
                .orElse(false);
        openTelemetryAppender.setCaptureMarkerAttribute(captureMarkerAttribute);

        String captureMdcAttributes = binder
                .bind(LogbackAppenderProperties.CONFIG_PREFIX + ".capture-mdc-attributes",
                        String.class)
                .orElse(null);
        openTelemetryAppender.setCaptureMdcAttributes(captureMdcAttributes);

        Integer numLogsCapturedBeforeOtelInstall = binder
                .bind(LogbackAppenderProperties.CONFIG_PREFIX + ".num-logs-captured-before-otel-install",
                        Integer.class)
                .orElse(1000);
        openTelemetryAppender.setNumLogsCapturedBeforeOtelInstall(numLogsCapturedBeforeOtelInstall);
    }

    private boolean isAssignableFrom(@Nullable Class<?> type, Class<?>... supportedTypes) {
        if (type != null) {
            for (Class<?> supportedType : supportedTypes) {
                if (supportedType.isAssignableFrom(type)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean shouldRegisterLogbackAppender() {
        return isLogbackPresent() && isOpenTelemetryPresent() && isLogbackAppenderPresent();
    }

    private boolean isLogbackPresent() {
        return ClassUtils.isPresent("ch.qos.logback.core.Appender", null);
    }

    private boolean isOpenTelemetryPresent() {
        return ClassUtils.isPresent("io.opentelemetry.api.OpenTelemetry", null);
    }

    private boolean isLogbackAppenderPresent() {
        return ClassUtils.isPresent("io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender", null);
    }

    private boolean isLogbackInstrumentationEnabled(Binder binder) {
        return binder.bind(LogbackAppenderProperties.CONFIG_PREFIX + ".enabled", Boolean.class)
                .orElse(true);
    }

    private boolean isOpenTelemetryEnabled(Binder binder) {
        return binder.bind("arconia.opentelemetry.enabled", Boolean.class).orElse(true);
    }

    @Override
    public int getOrder() {
        return LoggingApplicationListener.DEFAULT_ORDER + 1;
    }

}
