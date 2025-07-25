package io.arconia.opentelemetry.autoconfigure;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.trace.SdkTracerProvider;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for {@link OpenTelemetry}.
 */
@AutoConfiguration(before = org.springframework.boot.actuate.autoconfigure.opentelemetry.OpenTelemetryAutoConfiguration.class)
@ConditionalOnClass({ OpenTelemetry.class, OpenTelemetrySdk.class })
@EnableConfigurationProperties(OpenTelemetryProperties.class)
public class OpenTelemetryAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(OpenTelemetry.class)
    @ConditionalOnOpenTelemetry
    OpenTelemetrySdk openTelemetrySdk(ObjectProvider<SdkLoggerProvider> loggerProvider,
                                ObjectProvider<SdkMeterProvider> meterProvider,
                                ObjectProvider<SdkTracerProvider> tracerProvider,
                                ObjectProvider<ContextPropagators> propagators
    ) {
        OpenTelemetrySdkBuilder openTelemetryBuilder = OpenTelemetrySdk.builder();
        loggerProvider.ifAvailable(openTelemetryBuilder::setLoggerProvider);
        meterProvider.ifAvailable(openTelemetryBuilder::setMeterProvider);
        tracerProvider.ifAvailable(openTelemetryBuilder::setTracerProvider);
        propagators.ifAvailable(openTelemetryBuilder::setPropagators);
        return openTelemetryBuilder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnOpenTelemetry(enabled = false)
    OpenTelemetry noopOpenTelemetry() {
        return OpenTelemetry.noop();
    }

    @Bean
    @ConditionalOnMissingBean
    Clock clock() {
        return Clock.getDefault();
    }

}
