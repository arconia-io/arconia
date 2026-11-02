package io.arconia.opentelemetry.logback.autoconfigure;

import io.opentelemetry.instrumentation.api.config.IncludeExclude;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;

import org.jspecify.annotations.Nullable;
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
import org.springframework.util.ClassUtils;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;

import io.arconia.opentelemetry.autoconfigure.OpenTelemetryProperties;
import io.arconia.opentelemetry.autoconfigure.exporter.OpenTelemetryExporterProperties;
import io.arconia.opentelemetry.autoconfigure.logs.OpenTelemetryLoggingProperties;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.OpenTelemetryLoggingExporterProperties;

/**
 * An {@link ApplicationListener} that configures the OpenTelemetry Logback appender
 * with the root Logback logger.
 */
class LogbackOpenTelemetryBridgeApplicationListener implements GenericApplicationListener {

    static final String APPENDER_NAME = "OpenTelemetry";

    private static final Class<?>[] EVENT_TYPES = {ApplicationEnvironmentPreparedEvent.class};
    private static final Class<?>[] SOURCE_TYPES = {ApplicationContext.class, SpringApplication.class};

    @Override
    public boolean supportsEventType(ResolvableType eventType) {
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
        if (!isOpenTelemetryEnabled(binder) || !isLoggingExportEnabled(binder)) {
            return;
        }

        LogbackOpenTelemetryBridgeProperties properties = binder
                .bind(LogbackOpenTelemetryBridgeProperties.CONFIG_PREFIX, LogbackOpenTelemetryBridgeProperties.class)
                .orElseGet(LogbackOpenTelemetryBridgeProperties::new);
        if (!properties.isEnabled()) {
            return;
        }

        OpenTelemetryAppender openTelemetryAppender = new OpenTelemetryAppender();
        configureOpenTelemetryAppender(openTelemetryAppender, properties);
        openTelemetryAppender.start();

        // An application started more than once in the same JVM, such as in tests, gets a single appender.
        Logger rootLogbackLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        Appender<ILoggingEvent> previousAppender = rootLogbackLogger.getAppender(APPENDER_NAME);
        if (previousAppender != null) {
            rootLogbackLogger.detachAppender(previousAppender);
            previousAppender.stop();
        }
        rootLogbackLogger.addAppender(openTelemetryAppender);
    }

    private void configureOpenTelemetryAppender(OpenTelemetryAppender openTelemetryAppender, LogbackOpenTelemetryBridgeProperties properties) {
        openTelemetryAppender.setName(APPENDER_NAME);
        openTelemetryAppender.setCaptureArguments(properties.isCaptureArguments());
        openTelemetryAppender.setCaptureTemplate(properties.isCaptureTemplate());
        openTelemetryAppender.setCaptureCodeAttributes(properties.isCaptureCodeAttributes());
        openTelemetryAppender.setCaptureExperimentalAttributes(properties.isCaptureExperimentalAttributes());
        openTelemetryAppender.setCaptureMarkerAttribute(properties.isCaptureMarkerAttribute());

        IncludeExclude structuredAttributes = toIncludeExclude(properties.getStructuredAttributes());
        openTelemetryAppender.setKeyValuePairAttributes(structuredAttributes);
        openTelemetryAppender.setLogstashMarkerAttributes(structuredAttributes);
        openTelemetryAppender.setLogstashStructuredArgumentAttributes(structuredAttributes);
        openTelemetryAppender.setMdcAttributes(toIncludeExclude(properties.getMdcAttributes()));
        openTelemetryAppender.setLoggerContextAttributes(toIncludeExclude(properties.getLoggerContextAttributes()));

        openTelemetryAppender.setNumLogsCapturedBeforeOtelInstall(properties.getNumLogsCapturedBeforeOtelInstall());
    }

    /**
     * An empty selector captures nothing.
     */
    private IncludeExclude toIncludeExclude(LogbackOpenTelemetryBridgeProperties.AttributeSelector selector) {
        return IncludeExclude.builder()
                .setIncluded(selector.getIncluded())
                .setExcluded(selector.getExcluded())
                .build();
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
        return isLogbackPresent() && isOpenTelemetryPresent();
    }

    private boolean isLogbackPresent() {
        return ClassUtils.isPresent("ch.qos.logback.core.Appender", null);
    }

    private boolean isOpenTelemetryPresent() {
        return ClassUtils.isPresent("io.opentelemetry.api.OpenTelemetry", null);
    }

    private boolean isOpenTelemetryEnabled(Binder binder) {
        return binder.bind(OpenTelemetryProperties.CONFIG_PREFIX + ".enabled", Boolean.class).orElse(true);
    }

    /**
     * Whether logs are exported, which is the case for every exporter type but "none". The values are read
     * as text, since this listener runs whether or not Arconia OpenTelemetry is on the classpath.
     */
    private boolean isLoggingExportEnabled(Binder binder) {
        if (!binder.bind(OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".enabled", Boolean.class).orElse(true)) {
            return false;
        }
        String exporterType = binder.bind(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".type", String.class)
                .orElseGet(() -> binder.bind(OpenTelemetryExporterProperties.CONFIG_PREFIX + ".type", String.class).orElse("otlp"));
        return !"none".equalsIgnoreCase(exporterType.strip());
    }

    @Override
    public int getOrder() {
        return LoggingApplicationListener.DEFAULT_ORDER + 1;
    }

}
