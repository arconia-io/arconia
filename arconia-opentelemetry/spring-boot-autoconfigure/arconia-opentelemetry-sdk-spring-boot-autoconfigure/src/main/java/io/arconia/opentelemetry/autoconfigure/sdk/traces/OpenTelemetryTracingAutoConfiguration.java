package io.arconia.opentelemetry.autoconfigure.sdk.traces;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanLimits;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.actuate.autoconfigure.tracing.SdkTracerProviderBuilderCustomizer;
import org.springframework.boot.actuate.autoconfigure.tracing.TracingProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for OpenTelemetry tracing.
 */
@AutoConfiguration(before = org.springframework.boot.actuate.autoconfigure.tracing.OpenTelemetryTracingAutoConfiguration.class)
@ConditionalOnClass(SdkTracerProvider.class)
@ConditionalOnOpenTelemetryTracing
@EnableConfigurationProperties(OpenTelemetryTracingProperties.class)
public class OpenTelemetryTracingAutoConfiguration {

    public static final String INSTRUMENTATION_SCOPE_NAME = "org.springframework.boot";

    @Bean
    @ConditionalOnMissingBean
    SdkTracerProvider tracerProvider(Clock clock,
                                     Resource resource,
                                     Sampler sampler,
                                     SpanLimits spanLimits,
                                     ObjectProvider<SpanProcessor> spanProcessors,
                                     ObjectProvider<SdkTracerProviderBuilderCustomizer> customizers
    ) {
        SdkTracerProviderBuilder builder = SdkTracerProvider.builder()
                .setResource(resource)
                .setSampler(sampler)
                .setClock(clock)
                .setSpanLimits(spanLimits);
        spanProcessors.orderedStream().forEach(builder::addSpanProcessor);
        customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    Sampler sampler(OpenTelemetryTracingProperties properties, TracingProperties tracingProperties) {
        return switch (properties.getSampling().getStrategy()) {
            case ALWAYS_ON -> Sampler.alwaysOn();
            case ALWAYS_OFF -> Sampler.alwaysOff();
            case TRACE_ID_RATIO -> Sampler.traceIdRatioBased(tracingProperties.getSampling().getProbability());
            case PARENT_BASED_ALWAYS_ON -> Sampler.parentBased(Sampler.alwaysOn());
            case PARENT_BASED_ALWAYS_OFF -> Sampler.parentBased(Sampler.alwaysOff());
            case PARENT_BASED_TRACE_ID_RATIO -> Sampler.parentBased(Sampler.traceIdRatioBased(tracingProperties.getSampling().getProbability()));
        };
    }

    @Bean
    @ConditionalOnMissingBean
    SpanLimits spanLimits(OpenTelemetryTracingProperties properties) {
        return SpanLimits.builder()
                .setMaxNumberOfAttributes(properties.getSpanLimits().getMaxNumberOfAttributes())
                .setMaxNumberOfEvents(properties.getSpanLimits().getMaxNumberOfEvents())
                .setMaxNumberOfLinks(properties.getSpanLimits().getMaxNumberOfLinks())
                .setMaxNumberOfAttributesPerEvent(properties.getSpanLimits().getMaxNumberOfAttributesPerEvent())
                .setMaxNumberOfAttributesPerLink(properties.getSpanLimits().getMaxNumberOfAttributesPerLink())
                .setMaxAttributeValueLength(properties.getSpanLimits().getMaxAttributeValueLength())
                .build();
    }

//    @Bean
//    @ConditionalOnMissingBean
//    @ConditionalOnClass(OtelTracer.class)
//    BatchSpanProcessor micrometerSpanProcessor(OpenTelemetryTracingProperties properties,
//                                               ObjectProvider<SpanExporter> spanExporters,
//                                               ObjectProvider<SpanExportingPredicate> spanExportingPredicates,
//                                               ObjectProvider<SpanReporter> spanReporters,
//                                               ObjectProvider<SpanFilter> spanFilters,
//                                               ObjectProvider<MeterProvider> meterProvider
//    ) {
//        SpanExporter spanExporter = new CompositeSpanExporter(spanExporters.orderedStream().toList(), spanExportingPredicates.orderedStream().toList(),
//                spanReporters.orderedStream().toList(), spanFilters.orderedStream().toList());
//        BatchSpanProcessorBuilder spanProcessorBuilder = BatchSpanProcessor.builder(spanExporter)
//                .setExporterTimeout(properties.getProcessor().getExporterTimeout())
//                .setScheduleDelay(properties.getProcessor().getScheduleDelay())
//                .setMaxExportBatchSize(properties.getProcessor().getMaxExportBatchSize())
//                .setMaxQueueSize(properties.getProcessor().getMaxQueueSize());
//        if (properties.getProcessor().isMetrics()) {
//            meterProvider.ifAvailable(spanProcessorBuilder::setMeterProvider);
//        }
//        return spanProcessorBuilder.build();
//    }

    @Bean
    @ConditionalOnMissingBean
    ContextPropagators contextPropagators(ObjectProvider<TextMapPropagator> textMapPropagators) {
        return ContextPropagators.create(TextMapPropagator.composite(textMapPropagators.orderedStream().toList()));
    }

    @Bean
    @ConditionalOnMissingBean
    Tracer otelTracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer(INSTRUMENTATION_SCOPE_NAME, SpringBootVersion.getVersion());
    }

}
