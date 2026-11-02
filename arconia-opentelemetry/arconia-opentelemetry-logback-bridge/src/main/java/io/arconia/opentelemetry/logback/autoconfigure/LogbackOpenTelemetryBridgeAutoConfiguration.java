package io.arconia.opentelemetry.logback.autoconfigure;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

import ch.qos.logback.core.Appender;

import io.arconia.opentelemetry.autoconfigure.OpenTelemetryAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.exporter.OpenTelemetryExporterAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.ConditionalOnOpenTelemetryLoggingExporter;

/**
 * Auto-configuration for Logback OpenTelemetry Bridge.
 */
@AutoConfiguration(after = { OpenTelemetryAutoConfiguration.class, OpenTelemetryExporterAutoConfiguration.class })
@ConditionalOnClass(Appender.class)
@ConditionalOnProperty(prefix = LogbackOpenTelemetryBridgeProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
@Conditional(LogbackOpenTelemetryBridgeAutoConfiguration.LoggingExportEnabled.class)
@EnableConfigurationProperties(LogbackOpenTelemetryBridgeProperties.class)
public final class LogbackOpenTelemetryBridgeAutoConfiguration {

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

    /**
     * Logs are bridged for every exporter type but "none".
     */
    static class LoggingExportEnabled extends AnyNestedCondition {

        LoggingExportEnabled() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnOpenTelemetryLoggingExporter("console")
        static class ConsoleLoggingExportEnabled {}

        @ConditionalOnOpenTelemetryLoggingExporter("otlp")
        static class OtlpLoggingExportEnabled {}

    }

}
