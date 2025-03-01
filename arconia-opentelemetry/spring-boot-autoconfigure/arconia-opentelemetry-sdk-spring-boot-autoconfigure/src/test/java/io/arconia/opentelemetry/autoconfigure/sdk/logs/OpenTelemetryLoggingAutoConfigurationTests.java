package io.arconia.opentelemetry.autoconfigure.sdk.logs;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.logs.LogLimits;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.resources.Resource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.logging.SdkLoggerProviderBuilderCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link OpenTelemetryLoggingAutoConfiguration}.
 */
class OpenTelemetryLoggingAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenTelemetryLoggingAutoConfiguration.class))
            .withPropertyValues("arconia.otel.enabled=true")
            .withBean(Clock.class, Clock::getDefault)
            .withBean(Resource.class, Resource::empty);

    @Test
    void autoConfigurationNotActivatedWhenOpenTelemetryDisabled() {
        contextRunner
            .withPropertyValues("arconia.otel.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(SdkLoggerProvider.class));
    }

    @Test
    void autoConfigurationNotActivatedWhenLogsDisabled() {
        contextRunner
            .withPropertyValues("arconia.otel.logs.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(SdkLoggerProvider.class));
    }

    @Test
    void autoConfigurationNotActivatedWhenLoggerProviderClassMissing() {
        contextRunner.withClassLoader(new FilteredClassLoader(SdkLoggerProvider.class))
                .run(context -> assertThat(context).doesNotHaveBean(SdkLoggerProvider.class));
    }

    @Test
    void loggerProviderAvailableWithDefaultConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(SdkLoggerProvider.class);
            assertThat(context).hasSingleBean(LogLimits.class);
            assertThat(context).hasSingleBean(BatchLogRecordProcessor.class);
        });
    }

    @Test
    void customLogLimitsConfigurationApplied() {
        contextRunner
            .withPropertyValues(
                "arconia.otel.logs.log-limits.max-attribute-value-length=100",
                "arconia.otel.logs.log-limits.max-number-of-attributes=50"
            )
            .run(context -> {
                LogLimits logLimits = context.getBean(LogLimits.class);
                assertThat(logLimits.getMaxAttributeValueLength()).isEqualTo(100);
                assertThat(logLimits.getMaxNumberOfAttributes()).isEqualTo(50);
            });
    }

    @Test
    void customBatchProcessorConfigurationApplied() {
        contextRunner
            .withPropertyValues(
                "arconia.otel.logs.processor.export-timeout=10s",
                "arconia.otel.logs.processor.schedule-delay=5s",
                "arconia.otel.logs.processor.max-export-batch-size=512",
                "arconia.otel.logs.processor.max-queue-size=2048",
                "arconia.otel.logs.processor.metrics=false"
            )
            .withBean(MeterProvider.class, () -> mock(MeterProvider.class))
            .run(context -> {
                assertThat(context).hasSingleBean(BatchLogRecordProcessor.class);
            });
    }

    @Test
    void customLoggerProviderBuilderCustomizerApplied() {
        contextRunner
            .withUserConfiguration(CustomLoggerProviderConfiguration.class)
            .run(context -> {
                assertThat(context).hasSingleBean(SdkLoggerProvider.class);
                assertThat(context).hasSingleBean(SdkLoggerProviderBuilderCustomizer.class);
            });
    }

    @Test
    void customLogRecordProcessorTakesPrecedence() {
        contextRunner
            .withUserConfiguration(CustomLogRecordProcessorConfiguration.class)
            .run(context -> {
                assertThat(context).hasSingleBean(LogRecordProcessor.class);
                assertThat(context.getBean(LogRecordProcessor.class))
                    .isSameAs(context.getBean(CustomLogRecordProcessorConfiguration.class).customLogRecordProcessor());
            });
    }

    @Test
    void customLogRecordExporterAvailable() {
        contextRunner
            .withUserConfiguration(CustomLogRecordExporterConfiguration.class)
            .run(context -> {
                assertThat(context).hasSingleBean(LogRecordExporter.class);
                assertThat(context).hasSingleBean(BatchLogRecordProcessor.class);
            });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomLoggerProviderConfiguration {

        private final SdkLoggerProviderBuilderCustomizer customizer = mock(SdkLoggerProviderBuilderCustomizer.class);

        @Bean
        SdkLoggerProviderBuilderCustomizer customLoggerProviderBuilderCustomizer() {
            return customizer;
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class CustomLogRecordProcessorConfiguration {

        private final BatchLogRecordProcessor customLogRecordProcessor = mock(BatchLogRecordProcessor.class);

        @Bean
        BatchLogRecordProcessor customLogRecordProcessor() {
            return customLogRecordProcessor;
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class CustomLogRecordExporterConfiguration {

        private final LogRecordExporter customLogRecordExporter = mock(LogRecordExporter.class);

        @Bean
        LogRecordExporter customLogRecordExporter() {
            return customLogRecordExporter;
        }

    }

}
