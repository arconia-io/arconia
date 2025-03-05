package io.arconia.opentelemetry.autoconfigure.instrumentation.logback;

import io.arconia.opentelemetry.autoconfigure.instrumentation.ConditionalOnOpenTelemetryInstrumentation;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;

import ch.qos.logback.core.Appender;

import io.arconia.opentelemetry.autoconfigure.sdk.OpenTelemetryAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.sdk.logs.ConditionalOnOpenTelemetryLogging;
import io.arconia.opentelemetry.autoconfigure.sdk.logs.exporter.OpenTelemetryLoggingExporterProperties;

/**
 * Auto-configuration for OpenTelemetry Logback appender.
 */
@AutoConfiguration(after = OpenTelemetryAutoConfiguration.class)
@ConditionalOnClass(Appender.class)
@ConditionalOnOpenTelemetryLogging
@ConditionalOnOpenTelemetryInstrumentation(LogbackAppenderProperties.INSTRUMENTATION_NAME)
@ConditionalOnProperty(prefix = OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX, name = "type", havingValue = "otlp", matchIfMissing = true)
@EnableConfigurationProperties(LogbackAppenderProperties.class)
public class LogbackAppenderInstrumentationAutoConfiguration {

    @Bean
    @ConditionalOnBean(OpenTelemetry.class)
    ApplicationListener<ApplicationReadyEvent> logbackAppenderOnReady(OpenTelemetry openTelemetry) {
        return event -> OpenTelemetryAppender.install(openTelemetry);
    }

    @Bean
    @ConditionalOnBean(OpenTelemetry.class)
    ApplicationListener<ApplicationFailedEvent> logbackAppenderOnFailed(OpenTelemetry openTelemetry) {
        return event -> OpenTelemetryAppender.install(openTelemetry);
    }

}
