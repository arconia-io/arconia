package io.arconia.opentelemetry.autoconfigure.metrics;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.metrics.ExemplarFilter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.export.CardinalityLimitSelector;
import io.opentelemetry.sdk.resources.Resource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.micrometer.tracing.autoconfigure.TracingProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for OpenTelemetry metrics.
 */
@AutoConfiguration
@ConditionalOnOpenTelemetryMetrics
@EnableConfigurationProperties({ OpenTelemetryMetricsProperties.class, TracingProperties.class })
public final class OpenTelemetryMetricsAutoConfiguration {

    public static final String INSTRUMENTATION_SCOPE_NAME = "org.springframework.boot";

    @Bean
    @ConditionalOnMissingBean
    SdkMeterProvider meterProvider(Clock clock,
                                   ExemplarFilter exemplarFilter,
                                   Resource resource,
                                   ObjectProvider<OpenTelemetryMeterProviderBuilderCustomizer> customizers
    ) {
        SdkMeterProviderBuilder builder = SdkMeterProvider.builder()
                .setClock(clock)
                .setExemplarFilter(exemplarFilter)
                .setResource(resource);
        customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    CardinalityLimitSelector cardinalityLimitSelector(OpenTelemetryMetricsProperties properties) {
        return instrumentType -> properties.getCardinalityLimit();
    }

    /**
     * The exemplars follow the same Spring Boot property that configures them for the
     * Micrometer registries, so that one setting applies to all the metrics of the application.
     */
    @Bean
    @ConditionalOnMissingBean
    ExemplarFilter exemplarFilter(TracingProperties tracingProperties) {
        return switch (tracingProperties.getExemplars().getInclude()) {
            case ALL -> ExemplarFilter.alwaysOn();
            case NONE -> ExemplarFilter.alwaysOff();
            case SAMPLED_TRACES -> ExemplarFilter.traceBased();
        };
    }

    @Bean
    @ConditionalOnMissingBean
    Meter meter(OpenTelemetry openTelemetry) {
        return openTelemetry.meterBuilder(INSTRUMENTATION_SCOPE_NAME)
                .setInstrumentationVersion(SpringBootVersion.getVersion())
                .build();
    }

}
