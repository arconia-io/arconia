package io.arconia.opentelemetry.autoconfigure.sdk.logs;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.logs.LogLimits;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessorBuilder;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.resources.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.logging.SdkLoggerProviderBuilderCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for OpenTelemetry logging.
 */
@AutoConfiguration
@ConditionalOnClass(SdkLoggerProvider.class)
@ConditionalOnOpenTelemetryLogging
@EnableConfigurationProperties(OpenTelemetryLoggingProperties.class)
public class OpenTelemetryLoggingAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(OpenTelemetryLoggingAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    SdkLoggerProvider loggerProvider(Clock clock,
                                     LogLimits logLimits,
                                     Resource resource,
                                     ObjectProvider<LogRecordProcessor> logRecordProcessors,
                                     ObjectProvider<OpenTelemetryLoggerProviderBuilderCustomizer> customizers,
                                     ObjectProvider<SdkLoggerProviderBuilderCustomizer> sdkCustomizers
    ) {
        SdkLoggerProviderBuilder loggerProviderBuilder = SdkLoggerProvider.builder()
                .setClock(clock)
                .setLogLimits(() -> logLimits)
                .setResource(resource);
        logRecordProcessors.orderedStream().forEach(loggerProviderBuilder::addLogRecordProcessor);
        customizers.orderedStream().forEach((customizer) -> customizer.customize(loggerProviderBuilder));
        sdkCustomizers.orderedStream().forEach((customizer) -> customizer.customize(loggerProviderBuilder));
        sdkCustomizers.ifAvailable(customizer -> logger.warn("""
                You are using Spring Boot's SdkLoggerProviderBuilderCustomizer to customize the SdkLoggerProviderBuilder.
                For better compatibility with Arconia OpenTelemetry, use the OpenTelemetryLoggerProviderBuilderCustomizer instead.
                """));
        return loggerProviderBuilder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    LogLimits logLimits(OpenTelemetryLoggingProperties properties) {
        return LogLimits.builder()
                .setMaxAttributeValueLength(properties.getLogLimits().getMaxAttributeValueLength())
                .setMaxNumberOfAttributes(properties.getLogLimits().getMaxNumberOfAttributes())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    BatchLogRecordProcessor logRecordProcessor(OpenTelemetryLoggingProperties properties, ObjectProvider<LogRecordExporter> logRecordExporters, ObjectProvider<MeterProvider> meterProvider) {
        BatchLogRecordProcessorBuilder builder = BatchLogRecordProcessor.builder(LogRecordExporter.composite(logRecordExporters.orderedStream().toList()))
            .setExporterTimeout(properties.getProcessor().getExportTimeout())
            .setScheduleDelay(properties.getProcessor().getScheduleDelay())
            .setMaxExportBatchSize(properties.getProcessor().getMaxExportBatchSize())
            .setMaxQueueSize(properties.getProcessor().getMaxQueueSize());
        if (properties.getProcessor().isMetrics()) {
            meterProvider.ifAvailable(builder::setMeterProvider);
        }
        return builder.build();
    }

}
