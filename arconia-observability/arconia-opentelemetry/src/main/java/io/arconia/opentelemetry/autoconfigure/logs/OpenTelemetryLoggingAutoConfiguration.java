package io.arconia.opentelemetry.autoconfigure.logs;

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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for OpenTelemetry logging.
 */
@AutoConfiguration(before = org.springframework.boot.actuate.autoconfigure.logging.OpenTelemetryLoggingAutoConfiguration.class)
@ConditionalOnOpenTelemetryLogging
@EnableConfigurationProperties(OpenTelemetryLoggingProperties.class)
public final class OpenTelemetryLoggingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    SdkLoggerProvider loggerProvider(Clock clock,
                                     LogLimits logLimits,
                                     Resource resource,
                                     ObjectProvider<LogRecordProcessor> logRecordProcessors,
                                     ObjectProvider<OpenTelemetryLoggerProviderBuilderCustomizer> customizers
    ) {
        SdkLoggerProviderBuilder loggerProviderBuilder = SdkLoggerProvider.builder()
                .setClock(clock)
                .setLogLimits(() -> logLimits)
                .setResource(resource);
        logRecordProcessors.orderedStream().forEach(loggerProviderBuilder::addLogRecordProcessor);
        customizers.orderedStream().forEach((customizer) -> customizer.customize(loggerProviderBuilder));
        return loggerProviderBuilder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    LogLimits logLimits(OpenTelemetryLoggingProperties properties) {
        return LogLimits.builder()
                .setMaxAttributeValueLength(properties.getLimits().getMaxAttributeValueLength())
                .setMaxNumberOfAttributes(properties.getLimits().getMaxNumberOfAttributes())
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
