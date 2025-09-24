package io.arconia.opentelemetry.autoconfigure.metrics;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.export.CardinalityLimitSelector;
import io.opentelemetry.sdk.metrics.internal.SdkMeterProviderUtil;
import io.opentelemetry.sdk.metrics.internal.exemplar.ExemplarFilter;
import io.opentelemetry.sdk.resources.Resource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for OpenTelemetry metrics.
 */
@AutoConfiguration
@ConditionalOnClass(SdkMeterProvider.class)
@ConditionalOnOpenTelemetryMetrics
@EnableConfigurationProperties(OpenTelemetryMetricsProperties.class)
public final class OpenTelemetryMetricsAutoConfiguration {

    public static final String INSTRUMENTATION_SCOPE_NAME = "org.springframework.boot";

    @Bean
    @ConditionalOnMissingBean
    SdkMeterProvider meterProvider(Clock clock,
                                   ExemplarFilter exemplarFilter,
                                   OpenTelemetryMetricsProperties properties,
                                   Resource resource,
                                   ObjectProvider<OpenTelemetryMeterProviderBuilderCustomizer> customizers
    ) {
        SdkMeterProviderBuilder builder = SdkMeterProvider.builder()
                .setClock(clock)
                .setResource(resource);
        if (properties.getExemplars().isEnabled()) {
            // SDK implementation is still experimental, so we need to use the internal utility method.
            SdkMeterProviderUtil.setExemplarFilter(builder, exemplarFilter);
        }
        customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    CardinalityLimitSelector cardinalityLimitSelector(OpenTelemetryMetricsProperties properties) {
        return instrumentType -> properties.getCardinalityLimit();
    }

    @Bean
    @ConditionalOnMissingBean
    ExemplarFilter exemplarFilter(OpenTelemetryMetricsProperties properties) {
        return switch (properties.getExemplars().getFilter()) {
            case ALWAYS_ON -> ExemplarFilter.alwaysOn();
            case ALWAYS_OFF -> ExemplarFilter.alwaysOff();
            case TRACE_BASED -> ExemplarFilter.traceBased();
        };
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(OpenTelemetry.class)
    Meter meter(OpenTelemetry openTelemetry) {
        return openTelemetry.getMeter(INSTRUMENTATION_SCOPE_NAME);
    }

}
